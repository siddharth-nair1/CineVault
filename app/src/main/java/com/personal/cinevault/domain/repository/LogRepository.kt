package com.personal.cinevault.domain.repository

import com.personal.cinevault.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user watch-log entries — defined in the domain
 * layer, implemented in the data layer by
 * [com.personal.cinevault.data.repository.LogRepositoryImpl].
 */
interface LogRepository {

    /**
     * Observe all log entries as a reactive stream, ordered by most recently
     * watched first. Emits a new list whenever the underlying table changes.
     */
    fun getAllLogs(): Flow<List<LogEntry>>

    /**
     * Observe the log entry for a specific TMDB movie, or emit `null` if the
     * movie has not been logged yet.
     */
    fun getLogForMovie(tmdbMovieId: Int): Flow<LogEntry?>

    /**
     * Insert or update a log entry. If an entry with the same [LogEntry.id]
     * already exists, it is replaced; otherwise a new row is inserted.
     */
    suspend fun upsertLog(entry: LogEntry)

    /**
     * Permanently delete a log entry. Uses [LogEntry.id] to identify the row.
     */
    suspend fun deleteLog(entry: LogEntry)

    /**
     * Load a single log entry by its local database id, or `null` if not found.
     * Used by edit mode to pre-populate the [LogMovieViewModel] form.
     */
    suspend fun getLogById(id: Int): LogEntry?
}
