package com.personal.cinevault.domain.model

/**
 * Aggregated viewing statistics derived from all of a user's log entries.
 *
 * All fields are pure data — no framework dependencies.
 * Computed by [com.personal.cinevault.domain.usecase.GetUserStatsUseCase].
 */
data class UserStats(
    /** Total number of unique film log entries. */
    val totalFilmsWatched: Int,

    /** Films logged with a watchedDate in the current calendar year. */
    val filmsThisYear: Int,

    /**
     * Approximate total viewing time in whole hours, summed from
     * [Movie.runtime] (minutes) across all log entries.
     * Entries whose runtime is null are excluded from the sum.
     */
    val totalHoursWatched: Int,

    /**
     * Mean of all non-null, non-zero ratings stored on a 0–10 scale,
     * or null if no films have been rated.
     */
    val averageRating: Double?,

    /** Number of films marked as "liked". */
    val totalLiked: Int,

    /** Number of log entries marked as rewatches. */
    val totalRewatches: Int,

    /**
     * The genre appearing most frequently across all logged films, or null if
     * no genre data is available. Ties are broken by the first genre
     * encountered in iteration order.
     */
    val favouriteGenre: String?,

    /**
     * Distribution of star ratings given by the user.
     *
     * Keys are the display-scale rating (0.5–5.0 in 0.5 increments after
     * converting from storage scale 1–10), values are the count of films
     * rated at that level.
     * Only ratings that were actually given (non-null, > 0) are included.
     */
    val ratingDistribution: Map<Float, Int>,

    /**
     * The decade in which the most films in the user's diary were released,
     * represented as the decade's start year (e.g. 1990 for 1990–1999).
     * Null if no release-year data is available.
     */
    val mostWatchedDecade: Int?,
)
