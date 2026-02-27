package com.personal.cinevault.domain.usecase

import com.personal.cinevault.domain.model.UserStats
import com.personal.cinevault.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Use case that streams [UserStats] computed live from the user's full
 * log history.
 *
 * Backed by [LogRepository.getAllLogs] so the returned [Flow] re-emits
 * automatically whenever the diary changes (new entries, edits, deletes).
 *
 * All computation is pure Kotlin collection operations — no database queries
 * beyond the single reactive [getAllLogs] subscription.
 */
class GetUserStatsUseCase(private val logRepository: LogRepository) {

    operator fun invoke(): Flow<UserStats> =
        logRepository.getAllLogs().map { entries ->

            val currentYear = LocalDate.now().year

            // ── Basic counts ──────────────────────────────────────────────────

            val totalFilmsWatched = entries.size

            val filmsThisYear = entries.count { entry ->
                entry.watchedDate
                    ?.take(4)               // "YYYY" from "YYYY-MM-DD"
                    ?.toIntOrNull()
                    ?.let { it == currentYear }
                    ?: false
            }

            // ── Runtime ───────────────────────────────────────────────────────

            val totalMinutes = entries.sumOf { it.movie.runtime ?: 0 }
            val totalHoursWatched = totalMinutes / 60

            // ── Ratings ───────────────────────────────────────────────────────

            // Filter to entries with an actual rating (non-null, > 0).
            val ratedEntries = entries.filter { it.rating != null && it.rating > 0f }

            val averageRating: Double? = if (ratedEntries.isEmpty()) null
            else ratedEntries.map { it.rating!!.toDouble() }.average()

            // Bucket ratings into 0.5-step display-scale keys (storage ÷ 2,
            // then rounded to nearest 0.5).
            val ratingDistribution: Map<Float, Int> = ratedEntries
                .groupingBy { entry ->
                    // Convert storage scale (0–10) to display scale (0.0–5.0)
                    // and snap to nearest 0.5 step.
                    val display = (entry.rating!! / 2f)
                    (Math.round(display * 2) / 2f)   // round to nearest 0.5
                }
                .eachCount()
                .toSortedMap()

            // ── Liked / Rewatch ────────────────────────────────────────────────

            val totalLiked     = entries.count { it.liked }
            val totalRewatches = entries.count { it.rewatch }

            // ── Favourite genre ───────────────────────────────────────────────

            // Flatten all genre lists, count occurrences, pick the most frequent.
            val favouriteGenre: String? = entries
                .flatMap { it.movie.genres }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key

            // ── Most watched decade ────────────────────────────────────────────

            // Map each entry's release year to its decade start (floor to 10).
            // Count entries per decade, pick the highest.
            val mostWatchedDecade: Int? = entries
                .mapNotNull { entry ->
                    entry.movie.releaseYear?.let { year ->
                        (year / 10) * 10   // e.g. 1994 → 1990
                    }
                }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key

            // ── Assemble ──────────────────────────────────────────────────────

            UserStats(
                totalFilmsWatched = totalFilmsWatched,
                filmsThisYear     = filmsThisYear,
                totalHoursWatched = totalHoursWatched,
                averageRating     = averageRating,
                totalLiked        = totalLiked,
                totalRewatches    = totalRewatches,
                favouriteGenre    = favouriteGenre,
                ratingDistribution = ratingDistribution,
                mostWatchedDecade = mostWatchedDecade,
            )
        }
}
