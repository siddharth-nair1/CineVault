package com.personal.cinevault.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
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
 * CineVault Room database to a user-chosen file in Google Drive (or any
 * SAF provider) using a persisted ACTION_CREATE_DOCUMENT URI.
 *
 * **Steps:**
 * 1. Read the backup file URI from [CineVaultPreferences]; bail out if unset.
 * 2. Run [MovieCacheManager.runMaintenance] to clean up stale cache rows before
 *    copying the database.
 * 3. Close [PersonalDatabase] so Room flushes its WAL and the SQLite file is
 *    in a consistent, copyable state.
 * 4. Overwrite the single persisted file URI with the latest database bytes.
 * 5. Re-open [PersonalDatabase] (Koin will rebuild the singleton on next access).
 * 6. Persist the backup date in [CineVaultPreferences].
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
        // 1. Resolve the persisted backup file URI.
        val fileUriStr = preferences.getBackupFileUri()
            ?: throw IllegalStateException("Backup file URI not set — skipping backup")

        val fileUri = Uri.parse(fileUriStr)

        // 2. Cache maintenance before closing the DB.
        cacheManager.runMaintenance()

        // 3. Close PersonalDatabase so the WAL is fully flushed.
        PersonalDatabase.getInstance(context).close()

        // 4. Overwrite the single backup file in Google Drive.
        val sourceFile = context.getDatabasePath(PersonalDatabase.DB_NAME)
        if (!sourceFile.exists()) {
            throw IllegalStateException("Source database not found: ${sourceFile.absolutePath}")
        }

        // "wt" mode = write + truncate, so we overwrite the file in-place.
        context.contentResolver
            .openOutputStream(fileUri, "wt")
            ?.use { out -> sourceFile.inputStream().use { inp -> inp.copyTo(out) } }
            ?: throw IllegalStateException("Cannot open output stream for $fileUri")

        // 5. Force the database singleton to rebuild on next access.
        PersonalDatabase.getInstance(context)

        // 6. Record successful backup date.
        val today = LocalDate.now().format(DATE_FMT)
        preferences.saveLastBackupDate(today)

        Log.i(TAG, "Backup completed successfully → $fileUri")
    }
}
