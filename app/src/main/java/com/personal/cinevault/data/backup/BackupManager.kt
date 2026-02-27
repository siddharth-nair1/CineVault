package com.personal.cinevault.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Application-level helper that schedules, cancels, and observes the
 * [BackupWorker] periodic task via [WorkManager].
 *
 * Inject via Koin:
 * ```kotlin
 * single { BackupManager(androidContext()) }
 * ```
 *
 * Usage:
 * ```kotlin
 * val manager: BackupManager by inject()
 * manager.scheduleDaily()           // call once e.g. from Application.onCreate
 * manager.getLastBackupStatus()     // observe in a ViewModel
 * manager.cancelBackup()            // called when user disables backup
 * ```
 */
class BackupManager(private val context: Context) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    companion object {
        /** Unique name for the periodic backup task in WorkManager. */
        const val WORK_NAME = "cinevault_daily_backup"

        /** Target run hour — 2 AM local time. */
        private const val TARGET_HOUR = 2

        /** 24-hour repeat interval for the periodic work request. */
        private val INTERVAL = Duration.ofDays(1)
    }

    // ── Schedule ──────────────────────────────────────────────────────────────

    /**
     * Enqueue (or update) the daily backup periodic work request.
     *
     * - Runs once every 24 hours.
     * - Initial delay calculated so the first run lands at the next 2 AM local.
     * - Requires: CONNECTED network + battery not low.
     * - Uses [ExistingPeriodicWorkPolicy.UPDATE] so calling this again
     *   (e.g. after the user changes the enabled flag) updates constraints
     *   without losing the run history.
     */
    fun scheduleDaily() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val initialDelay = calculateInitialDelay()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            repeatInterval = INTERVAL.toMinutes(),
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    /**
     * Cancel the scheduled daily backup. Safe to call when backup is disabled
     * by the user; does nothing if the work was never enqueued.
     */
    fun cancelBackup() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    /**
     * Returns a [Flow] of the most recent [WorkInfo] for the backup task,
     * or `null` if the work has never been enqueued.
     *
     * Observe this in a ViewModel to display the last run state and time
     * on the Settings / Backup screen.
     */
    fun getLastBackupStatus(): Flow<WorkInfo?> =
        workManager
            .getWorkInfosForUniqueWorkFlow(WORK_NAME)
            .map { infos -> infos.firstOrNull() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Calculates the [Duration] from now until the next occurrence of
     * [TARGET_HOUR]:00 local time. If it is already past [TARGET_HOUR] today,
     * the delay targets 2 AM tomorrow.
     */
    private fun calculateInitialDelay(): Duration {
        val now    = LocalDateTime.now()
        var next2am = now.toLocalDate().atTime(TARGET_HOUR, 0)

        // If we are at or past 2 AM today, push to tomorrow.
        if (!now.isBefore(next2am)) {
            next2am = next2am.plusDays(1)
        }

        return Duration.between(now, next2am)
    }
}
