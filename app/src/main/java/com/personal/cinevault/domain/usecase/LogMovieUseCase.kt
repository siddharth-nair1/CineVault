package com.personal.cinevault.domain.usecase

import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.repository.LogRepository

/**
 * Use case that persists a single watch-log entry.
 *
 * The caller (typically the ViewModel) is responsible for constructing the
 * fully-formed [LogEntry] — including any rating scale conversion or date
 * formatting — before invoking this use case.
 *
 * @param logRepository The repository responsible for persisting log entries.
 */
class LogMovieUseCase(private val logRepository: LogRepository) {

    /**
     * Upsert [entry] into the diary. If a row with the same id already exists
     * it is replaced; otherwise a new row is created.
     *
     * @return [Result.success] on a successful upsert, [Result.failure] on error.
     */
    suspend operator fun invoke(entry: LogEntry): Result<Unit> =
        runCatching { logRepository.upsertLog(entry) }
}
