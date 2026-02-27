package com.personal.cinevault.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.personal.cinevault.data.backup.BackupManager
import com.personal.cinevault.data.backup.CineVaultPreferences
import com.personal.cinevault.data.local.PersonalDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "SettingsViewModel"

class SettingsViewModel(
    private val preferences: CineVaultPreferences,
    private val backupManager: BackupManager,
) : ViewModel() {

    // ── Theme ─────────────────────────────────────────────────────────────────

    val isDarkTheme: StateFlow<Boolean> = preferences.observeTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onThemeToggle(dark: Boolean) {
        viewModelScope.launch { preferences.setDarkTheme(dark) }
    }

    // ── Backup folder ─────────────────────────────────────────────────────────

    val backupFolderUri: StateFlow<String?> = preferences.observeBackupFolderUri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastBackupDate: StateFlow<String?> = preferences.observeLastBackupDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Build an [Intent] for the system folder-picker (ACTION_OPEN_DOCUMENT_TREE).
     * The UI should launch this via [ActivityResultLauncher].
     */
    fun getBackupFolderIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION  or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }

    /**
     * Called after the user picks a folder. Persists the URI with a
     * persistable permission grant so it survives app restarts, then
     * (re-)schedules the daily backup worker.
     */
    fun onBackupFolderSelected(context: Context, uri: Uri) {
        // Take persistable permission so we can access the folder after restart.
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch {
            preferences.saveBackupFolderUri(uri.toString())
            preferences.setBackupEnabled(true)
            backupManager.scheduleDaily()
        }
    }

    // ── Backup status ─────────────────────────────────────────────────────────

    /**
     * Human-readable status string derived from the WorkManager [WorkInfo].
     * Defaults to "Not scheduled" until the worker runs.
     */
    val backupStatus: StateFlow<String> = backupManager.getLastBackupStatus()
        .map { info ->
            when (info?.state) {
                WorkInfo.State.ENQUEUED  -> "Scheduled"
                WorkInfo.State.RUNNING   -> "Running…"
                WorkInfo.State.SUCCEEDED -> "Completed"
                WorkInfo.State.FAILED    -> "Failed — tap Backup Now to retry"
                WorkInfo.State.BLOCKED   -> "Waiting for constraints"
                WorkInfo.State.CANCELLED -> "Cancelled"
                null                     -> "Not scheduled"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Not scheduled")

    // ── Manual backup ─────────────────────────────────────────────────────────

    private val _manualBackupState = MutableStateFlow<ManualBackupState>(ManualBackupState.Idle)
    val manualBackupState: StateFlow<ManualBackupState> = _manualBackupState.asStateFlow()

    /**
     * Kick off a one-shot backup on the calling coroutine. Writes progress to
     * [manualBackupState] so the UI can show a spinner / result snackbar.
     */
    fun runManualBackup(context: Context) {
        if (_manualBackupState.value is ManualBackupState.Running) return
        viewModelScope.launch {
            _manualBackupState.value = ManualBackupState.Running
            try {
                val folderUriStr = preferences.getBackupFolderUri()
                    ?: run {
                        _manualBackupState.value = ManualBackupState.Error("No backup folder set")
                        return@launch
                    }

                val today = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val backupName = "cinevault_backup_$today.db"
                val folderUri = Uri.parse(folderUriStr)
                val folderDoc = androidx.documentfile.provider.DocumentFile
                    .fromTreeUri(context, folderUri)
                    ?: run {
                        _manualBackupState.value = ManualBackupState.Error("Cannot open backup folder")
                        return@launch
                    }

                // Overwrite same-day file
                folderDoc.findFile(backupName)?.delete()
                val destDoc = folderDoc.createFile("application/octet-stream", backupName)
                    ?: run {
                        _manualBackupState.value = ManualBackupState.Error("Cannot create backup file")
                        return@launch
                    }

                val sourceFile = context.getDatabasePath(PersonalDatabase.DB_NAME)
                context.contentResolver.openOutputStream(destDoc.uri)?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                }

                preferences.saveLastBackupDate(today)
                _manualBackupState.value = ManualBackupState.Success("Backup saved: $backupName")
                Log.i(TAG, "Manual backup → $backupName")
            } catch (e: Exception) {
                Log.e(TAG, "Manual backup failed", e)
                _manualBackupState.value = ManualBackupState.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun clearManualBackupState() { _manualBackupState.value = ManualBackupState.Idle }

    // ── Restore ───────────────────────────────────────────────────────────────

    /**
     * Build an [Intent] for the system file-picker to select a `.db` backup
     * file. The UI should launch this via [ActivityResultLauncher].
     */
    fun getRestoreFileIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            // Allow .db files — some providers report them as octet-stream,
            // others may need a wildcard.
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "*/*"))
        }

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

    /**
     * Copy the selected backup file over the live database.
     * Closes the Room instance first, copies, then re-opens.
     */
    fun restoreFromBackup(context: Context, uri: Uri) {
        if (_restoreState.value is RestoreState.Running) return
        viewModelScope.launch {
            _restoreState.value = RestoreState.Running
            try {
                val db = PersonalDatabase.getInstance(context)
                db.close()

                val destFile = context.getDatabasePath(PersonalDatabase.DB_NAME)
                // Ensure parent directory exists (it always should, but be safe)
                destFile.parentFile?.mkdirs()

                context.contentResolver.openInputStream(uri)?.use { inp ->
                    destFile.outputStream().use { out -> inp.copyTo(out) }
                } ?: throw IllegalStateException("Cannot open restore file")

                // Re-open the singleton (next access triggers rebuild)
                PersonalDatabase.getInstance(context)

                _restoreState.value = RestoreState.Success
                Log.i(TAG, "Restore completed from $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _restoreState.value = RestoreState.Error(e.message ?: "Restore failed")
                // Try to re-open DB even on failure so the app stays functional
                runCatching { PersonalDatabase.getInstance(context) }
            }
        }
    }

    fun clearRestoreState() { _restoreState.value = RestoreState.Idle }
}

// ── State sealed classes ───────────────────────────────────────────────────────

sealed class ManualBackupState {
    object Idle    : ManualBackupState()
    object Running : ManualBackupState()
    data class Success(val message: String) : ManualBackupState()
    data class Error(val message: String)   : ManualBackupState()
}

sealed class RestoreState {
    object Idle    : RestoreState()
    object Running : RestoreState()
    object Success : RestoreState()
    data class Error(val message: String) : RestoreState()
}
