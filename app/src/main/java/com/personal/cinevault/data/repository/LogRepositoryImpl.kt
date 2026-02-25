package com.personal.cinevault.data.repository

import com.personal.cinevault.data.local.dao.LogEntryDao
import com.personal.cinevault.data.local.toDomain
import com.personal.cinevault.data.local.toEntity
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [LogRepository] backed by Room via [LogEntryDao].
 *
 * All mapping between [LogEntryEntity] and [LogEntry] is delegated to the
 * extension functions in `LogEntryMappers.kt`.
 */
class LogRepositoryImpl(
    private val dao: LogEntryDao
) : LogRepository {

    /**
     * Observe all log entries as a reactive [Flow], mapping each entity to
     * the domain model. Emits a new list on every database change.
     */
    override fun getAllLogs(): Flow<List<LogEntry>> =
        dao.getAllLogs().map { entities -> entities.map { it.toDomain() } }

    /**
     * Observe the log entry for a specific TMDB movie, or emit `null` if none
     * exists. Maps the nullable entity to a nullable domain model.
     */
    override fun getLogForMovie(tmdbMovieId: Int): Flow<LogEntry?> =
        dao.getLogForMovie(tmdbMovieId).map { it?.toDomain() }

    /**
     * Insert or replace a log entry. [LogEntry] is mapped to [LogEntryEntity]
     * before passing to the DAO.
     */
    override suspend fun upsertLog(entry: LogEntry) =
        dao.upsert(entry.toEntity())

    /**
     * Delete a log entry by mapping [LogEntry] back to [LogEntryEntity] so
     * the DAO can identify the row by its primary key.
     */
    override suspend fun deleteLog(entry: LogEntry) =
        dao.delete(entry.toEntity())

    /**
     * Load a single log entry by its [id], or `null` if not found in the table.
     */
    override suspend fun getLogById(id: Int): LogEntry? =
        dao.getById(id)?.toDomain()
}
