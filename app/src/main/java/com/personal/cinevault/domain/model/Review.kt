package com.personal.cinevault.domain.model

/**
 * Clean domain model representing a user's written review for a movie.
 *
 * Entity ↔ domain mapping lives in `data/local/ReviewMappers.kt`.
 */
data class Review(

    /** Local database primary key (0 for unsaved reviews). */
    val id: Int = 0,

    /** TMDB movie ID — used to cross-reference remote data. */
    val tmdbMovieId: Int,

    /** Cached movie title at the time the review was written. */
    val movieTitle: String,

    /** Cached TMDB poster path (e.g. "/abc123.jpg"), or null if unavailable. */
    val posterPath: String? = null,

    /** The body text of the review. */
    val content: String,

    /** User's numerical rating out of 10, or null if unrated. */
    val rating: Float? = null,

    /** Whether this review contains plot spoilers. */
    val containsSpoilers: Boolean = false,

    /** Epoch-millis timestamp of when this review was created on device. */
    val createdAt: Long = System.currentTimeMillis(),
)
