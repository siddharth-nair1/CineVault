package com.personal.cinevault.domain.usecase

import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.LogRepository
import java.time.Instant
import java.time.ZoneId

/**
 * Use case that persists a single watch-log entry.
 *
 * Accepts UI-facing parameters (display-scale rating, epoch-millis date) and
 * handles all conversion before delegating to [LogRepository.upsertLog].
 *
 * @param logRepository The repository responsible for persisting log entries.
 */
class LogMovieUseCase(private val logRepository: LogRepository) {

    /**
     * Create and persist a [LogEntry] for the given [movie].
     *
     * @param movie           The movie being logged.
     * @param displayRating   Star rating on a 0.5–5.0 half-star scale, or null
     *                        if the user chose not to rate the film.
     * @param liked           Whether the user marked the film as "liked".
     * @param isRewatch       Whether this viewing was a rewatch.
     * @param watchedDateMs   Selected date as epoch-millis (from DatePicker).
     * @param review          Optional free-text review/note, or null/blank to omit.
     * @return [Result.success] on a successful upsert, [Result.failure] on error.
     */
    suspend operator fun invoke(
        movie: Movie,
        displayRating: Float?,
        liked: Boolean,
        isRewatch: Boolean,
        watchedDateMs: Long,
        review: String?
    ): Result<Unit> = runCatching {
        // Convert display-scale (0.5–5.0) to storage-scale (1.0–10.0).
        val storageRating = displayRating?.let { it * 2f }

        // Convert epoch millis → ISO-8601 date string ("YYYY-MM-DD").
        val watchedDate = Instant.ofEpochMilli(watchedDateMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        logRepository.upsertLog(
            LogEntry(
                movie = movie,
                rating = storageRating,
                liked = liked,
                rewatch = isRewatch,
                review = review?.trim()?.takeIf { it.isNotEmpty() },
                watchedDate = watchedDate
            )
        )
    }
}
