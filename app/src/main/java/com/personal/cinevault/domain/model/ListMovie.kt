package com.personal.cinevault.domain.model

/**
 * Domain model for a movie entry inside a user-created list.
 *
 * Caches just enough display metadata so the list screen can render movie
 * cards without hitting the network or the TMDB cache database.
 *
 * Mapping between [com.personal.cinevault.data.local.entity.ListMovieEntity]
 * and this model is done in `CineListMappers.kt`.
 */
data class ListMovie(

    /** ID of the [CineList] this movie belongs to. */
    val listId: Int,

    /** TMDB movie ID — globally unique identifier. */
    val tmdbMovieId: Int,

    /** Movie title cached at the time it was added to the list. */
    val movieTitle: String,

    /** TMDB poster path (e.g. "/abc123.jpg"), or `null` if unavailable. */
    val posterPath: String? = null,

    /** Cached TMDB vote average for display in list cards. */
    val voteAverage: Float? = null,

    /** Release year (e.g. "2023"), cached for display without network calls. */
    val releaseYear: String? = null,

    /** Display order within the list (lower = appears higher on screen). */
    val sortOrder: Int = 0,

    /** Epoch-millis timestamp of when the movie was added to this list. */
    val addedAt: Long = System.currentTimeMillis(),
)
