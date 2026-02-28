package com.personal.cinevault.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Top-level extension — creates a single DataStore instance per process.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinevault_preferences"
)

/**
 * Centralised DataStore accessor for all user preferences in CineVault.
 *
 * Register as a Koin singleton:
 * ```kotlin
 * single { CineVaultPreferences(androidContext()) }
 * ```
 */
class CineVaultPreferences(private val context: Context) {

    // ── Keys ─────────────────────────────────────────────────────────────────

    companion object PreferencesKeys {
        /** Key is intentionally kept as "backup_folder_uri" for backward compatibility. */
        val BACKUP_FILE_URI    = stringPreferencesKey("backup_folder_uri")
        val IS_DARK_THEME      = booleanPreferencesKey("is_dark_theme")
        val LAST_BACKUP_DATE   = stringPreferencesKey("last_backup_date")
        val IS_BACKUP_ENABLED  = booleanPreferencesKey("is_backup_enabled")
    }

    // ── Backup file URI ──────────────────────────────────────────────────────

    /** Persist the SAF file URI string created by ACTION_CREATE_DOCUMENT. */
    suspend fun saveBackupFileUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[BACKUP_FILE_URI] = uri
        }
    }

    /** Returns the stored backup file URI, or null if not yet set. */
    suspend fun getBackupFileUri(): String? =
        context.dataStore.data.first()[BACKUP_FILE_URI]

    /** Observe the backup file URI reactively. Emits null until a URI is saved. */
    fun observeBackupFileUri(): Flow<String?> =
        context.dataStore.data.map { it[BACKUP_FILE_URI] }

    // ── Dark theme ───────────────────────────────────────────────────────────

    /** Set the app's dark/light mode preference. */
    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DARK_THEME] = dark
        }
    }

    /**
     * Observe the dark-theme preference as a [Flow].
     * Emits `false` (system default) until the user has explicitly chosen.
     */
    fun observeTheme(): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[IS_DARK_THEME] ?: false
        }

    // ── Last backup date ─────────────────────────────────────────────────────

    /** Persist the ISO-8601 date string of the most recent successful backup. */
    suspend fun saveLastBackupDate(date: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_BACKUP_DATE] = date
        }
    }

    /** Returns the last backup date string, or null if no backup has been made. */
    suspend fun getLastBackupDate(): String? =
        context.dataStore.data.first()[LAST_BACKUP_DATE]

    /** Observe the last backup date reactively. */
    fun observeLastBackupDate(): Flow<String?> =
        context.dataStore.data.map { it[LAST_BACKUP_DATE] }

    // ── Backup enabled ───────────────────────────────────────────────────────

    /** Enable or disable the automatic backup feature. */
    suspend fun setBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_BACKUP_ENABLED] = enabled
        }
    }

    /** Returns true if automatic backup is turned on (defaults to false). */
    suspend fun isBackupEnabled(): Boolean =
        context.dataStore.data.first()[IS_BACKUP_ENABLED] ?: false

    /** Observe the backup-enabled flag reactively. */
    fun observeBackupEnabled(): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[IS_BACKUP_ENABLED] ?: false
        }
}
