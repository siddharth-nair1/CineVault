package com.personal.cinevault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.personal.cinevault.data.local.entity.LogEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [LogEntryEntity] / `log_entries` table.
 */
@Dao
interface LogEntryDao {

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Observe all log entries, ordered with the most recent watch date first.
     * Emits a new list whenever the underlying table changes.
     */
    @Query("SELECT * FROM log_entries ORDER BY watchedDate DESC, createdAt DESC")
    fun getAllLogs(): Flow<List<LogEntryEntity>>

    /**
     * Observe the single log entry for a specific movie, or `null` if the
     * movie has not been logged yet.
     */
    @Query("SELECT * FROM log_entries WHERE tmdbMovieId = :tmdbMovieId LIMIT 1")
    fun getLogForMovie(tmdbMovieId: Int): Flow<LogEntryEntity?>

    /**
     * Return the single log entry with the given primary key, or `null` if none
     * exists. Used by the edit-mode flow in [LogMovieViewModel].
     */
    @Query("SELECT * FROM log_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): LogEntryEntity?

    // ── Aggregate stats ──────────────────────────────────────────────────────

    /**
     * Observe the total number of distinct films the user has logged.
     * Emits whenever the `log_entries` table changes.
     */
    @Query("SELECT COUNT(DISTINCT tmdbMovieId) FROM log_entries")
    fun getTotalFilmsWatched(): Flow<Int>

    /**
     * Observe the user's average rating across all log entries that have a
     * non-null rating. Returns `null` when no rated entries exist.
     */
    @Query("SELECT AVG(rating) FROM log_entries WHERE rating IS NOT NULL")
    fun getAverageRating(): Flow<Float?>

    /**
     * Return the total number of log entries.
     *
     * Note: total minutes watched cannot be computed with a pure SQL query
     * here because runtime lives in the separate cache database. The
     * repository layer should combine this count with runtimes from
     * [com.personal.cinevault.data.local.dao.MovieCacheDao].
     */
    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun getTotalLogCount(): Int

    // ── Writes ───────────────────────────────────────────────────────────────

    /**
     * Insert or update a log entry.
     * If a row with the same primary key already exists it is replaced.
     */
    @Upsert
    suspend fun upsert(logEntry: LogEntryEntity)

    /**
     * Delete a specific log entry.
     */
    @Delete
    suspend fun delete(logEntry: LogEntryEntity)
}
