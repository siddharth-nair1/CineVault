package com.personal.cinevault.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.cinevault.data.local.MovieCacheManager
import com.personal.cinevault.data.local.PersonalDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * WorkManager [CoroutineWorker] that performs a safe daily backup of the
 * CineVault Room database to a user-chosen SAF (Storage Access Framework)
 * folder.
 *
 * **Steps:**
 * 1. Read the backup folder URI from [CineVaultPreferences]; bail out if unset.
 * 2. Run [MovieCacheManager.runMaintenance] to clean up stale cache rows before
 *    copying the database.
 * 3. Close [PersonalDatabase] so Room flushes its WAL and the SQLite file is
 *    in a consistent, copyable state.
 * 4. Copy `cinevault_personal.db` to the SAF folder as
 *    `cinevault_backup_YYYY-MM-DD.db`, overwriting any existing same-day file.
 * 5. Prune backup files in the SAF folder that are older than 7 days.
 * 6. Re-open [PersonalDatabase] (Koin will rebuild the singleton on next access).
 * 7. Persist the backup date in [CineVaultPreferences].
 *
 * On any exception: retry up to 3 total attempts, then fail permanently.
 */
class BackupWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val preferences: CineVaultPreferences by inject()
    private val cacheManager: MovieCacheManager    by inject()

    companion object {
        private const val TAG = "BackupWorker"

        /** Prefix used to identify backup files in the SAF folder. */
        private const val BACKUP_PREFIX = "cinevault_backup_"

        /** Extension of backup files. */
        private const val BACKUP_EXT = ".db"

        /** Keep backups no older than this many days. */
        private const val RETENTION_DAYS = 7L

        /** ISO-8601 date formatter for file names and retention logic. */
        private val DATE_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    // ── doWork ────────────────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        return try {
            backup()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Backup attempt $runAttemptCount failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    // ── Core backup logic ──────────────────────────────────────────────────────

    private suspend fun backup() {
        // 1. Resolve backup folder.
        val folderUriStr = preferences.getBackupFolderUri()
            ?: throw IllegalStateException("Backup folder URI not set — skipping backup")

        val folderUri  = Uri.parse(folderUriStr)
        val folderDoc  = DocumentFile.fromTreeUri(context, folderUri)
            ?: throw IllegalStateException("Cannot open backup folder: $folderUriStr")

        // 2. Cache maintenance before closing the DB.
        cacheManager.runMaintenance()

        // 3. Close PersonalDatabase so the WAL is fully flushed.
        PersonalDatabase.getInstance(context).close()

        // 4. Copy the database file to the SAF folder.
        val today      = LocalDate.now().format(DATE_FMT)
        val backupName = "$BACKUP_PREFIX$today$BACKUP_EXT"
        copyDatabaseToSaf(folderDoc, backupName)

        // 5. Prune backups older than RETENTION_DAYS.
        pruneOldBackups(folderDoc)

        // 6. Force the database singleton to be rebuilt on next access.
        //    PersonalDatabase.getInstance() will call buildDatabase() again
        //    the next time any repository accesses it.
        PersonalDatabase.getInstance(context)

        // 7. Record successful backup date.
        preferences.saveLastBackupDate(today)

        Log.i(TAG, "Backup completed successfully → $backupName")
    }

    // ── Copy helper ───────────────────────────────────────────────────────────

    /**
     * Copies `cinevault_personal.db` from the app's database directory to
     * [folder] via a [ContentResolver] output stream, overwriting any existing
     * file with [backupName].
     */
    private fun copyDatabaseToSaf(folder: DocumentFile, backupName: String) {
        // Overwrite same-day file if it already exists.
        folder.findFile(backupName)?.delete()

        val destDoc = folder.createFile("application/octet-stream", backupName)
            ?: throw IllegalStateException("Cannot create backup file: $backupName")

        val sourceFile = context.getDatabasePath(PersonalDatabase.DB_NAME)
        if (!sourceFile.exists()) {
            throw IllegalStateException("Source database not found: ${sourceFile.absolutePath}")
        }

        context.contentResolver.openOutputStream(destDoc.uri)?.use { out ->
            sourceFile.inputStream().use { inp -> inp.copyTo(out) }
        } ?: throw IllegalStateException("Cannot open output stream for ${destDoc.uri}")
    }

    // ── Pruning helper ────────────────────────────────────────────────────────

    /**
     * Deletes any file in [folder] whose name matches the backup pattern
     * and whose embedded date is older than [RETENTION_DAYS] days.
     */
    private fun pruneOldBackups(folder: DocumentFile) {
        val cutoff = LocalDate.now().minusDays(RETENTION_DAYS)
        folder.listFiles()
            .filter { doc ->
                val name = doc.name ?: return@filter false
                name.startsWith(BACKUP_PREFIX) && name.endsWith(BACKUP_EXT)
            }
            .forEach { doc ->
                val name = doc.name ?: return@forEach
                // Extract "yyyy-MM-dd" from "cinevault_backup_yyyy-MM-dd.db"
                val dateStr = name
                    .removePrefix(BACKUP_PREFIX)
                    .removeSuffix(BACKUP_EXT)
                val fileDate = runCatching { LocalDate.parse(dateStr, DATE_FMT) }.getOrNull()
                    ?: return@forEach
                if (fileDate.isBefore(cutoff)) {
                    val deleted = doc.delete()
                    Log.d(TAG, "Pruned old backup: $name (deleted=$deleted)")
                }
            }
    }
}
