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

    // ── Backup file URI ───────────────────────────────────────────────────────

    /** Observed by the UI to show the current backup file path. */
    val backupFileUri: StateFlow<String?> = preferences.observeBackupFileUri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastBackupDate: StateFlow<String?> = preferences.observeLastBackupDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Build an [Intent] using ACTION_CREATE_DOCUMENT so the user can create a
     * backup file directly inside Google Drive (or any provider).
     *
     * ACTION_CREATE_DOCUMENT works with Google Drive on all Android versions,
     * unlike ACTION_OPEN_DOCUMENT_TREE which is broken for Drive on Android 11+.
     */
    fun getBackupFileIntent(): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "cinevault_backup.db")
        }

    /**
     * Called after the user creates/selects the backup file.
     * Takes a persistable READ + WRITE grant so the URI survives app restarts,
     * then schedules the nightly WorkManager task.
     */
    fun onBackupFileSelected(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch {
            preferences.saveBackupFileUri(uri.toString())
            preferences.setBackupEnabled(true)
            backupManager.scheduleDaily()
        }
    }

    // ── Backup status ─────────────────────────────────────────────────────────

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
     * Overwrite the single saved backup file URI with a fresh copy of the database.
     * Does NOT require a folder; the URI already points to the exact file in Drive.
     */
    fun runManualBackup(context: Context) {
        if (_manualBackupState.value is ManualBackupState.Running) return
        viewModelScope.launch {
            _manualBackupState.value = ManualBackupState.Running
            try {
                val fileUriStr = preferences.getBackupFileUri()
                    ?: run {
                        _manualBackupState.value =
                            ManualBackupState.Error("No backup file set. Tap 'Set Backup File' first.")
                        return@launch
                    }

                val fileUri    = Uri.parse(fileUriStr)
                val sourceFile = context.getDatabasePath(PersonalDatabase.DB_NAME)
                if (!sourceFile.exists()) {
                    _manualBackupState.value = ManualBackupState.Error("Database file not found")
                    return@launch
                }

                // Close Room so the WAL is fully checkpointed before we copy.
                PersonalDatabase.getInstance(context).close()

                // Overwrite the file at the persisted URI (works with Google Drive).
                context.contentResolver
                    .openOutputStream(fileUri, "wt")  // "wt" = write + truncate
                    ?.use { out -> sourceFile.inputStream().use { inp -> inp.copyTo(out) } }
                    ?: throw IllegalStateException("Cannot open output stream for $fileUri")

                // Reopen the Room singleton.
                PersonalDatabase.getInstance(context)

                val today = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                preferences.saveLastBackupDate(today)

                _manualBackupState.value = ManualBackupState.Success("Backup saved to Google Drive")
                Log.i(TAG, "Manual backup → $fileUri")
            } catch (e: Exception) {
                Log.e(TAG, "Manual backup failed", e)
                // Make sure Room is back up even if backup failed.
                runCatching { PersonalDatabase.getInstance(context) }
                _manualBackupState.value = ManualBackupState.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun clearManualBackupState() { _manualBackupState.value = ManualBackupState.Idle }

    // ── Restore ───────────────────────────────────────────────────────────────

    /**
     * Build an [Intent] for the system file-picker to select a `.db` backup
     * file. ACTION_OPEN_DOCUMENT works fine with Google Drive — no change needed.
     */
    fun getRestoreFileIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
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
                destFile.parentFile?.mkdirs()

                context.contentResolver.openInputStream(uri)?.use { inp ->
                    destFile.outputStream().use { out -> inp.copyTo(out) }
                } ?: throw IllegalStateException("Cannot open restore file")

                PersonalDatabase.getInstance(context)

                _restoreState.value = RestoreState.Success
                Log.i(TAG, "Restore completed from $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _restoreState.value = RestoreState.Error(e.message ?: "Restore failed")
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
