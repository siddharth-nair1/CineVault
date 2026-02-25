package com.personal.cinevault.data.local

import com.personal.cinevault.data.local.entity.WatchlistEntity
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.model.WatchlistItem

// ─── WatchlistEntity → WatchlistItem ─────────────────────────────────────────

/**
 * Map a database [WatchlistEntity] to the clean domain [WatchlistItem].
 *
 * Like the log mapper, only the fields cached on the entity are populated.
 * Callers should enrich [WatchlistItem.movie] from the cache/network when
 * full detail (overview, backdrop, genres) is required.
 */
fun WatchlistEntity.toDomain(): WatchlistItem = WatchlistItem(
    id = id,
    movie = Movie(
        id = tmdbMovieId,
        title = movieTitle,
        overview = "",              // not stored on entity
        posterPath = posterPath,
        backdropPath = null,        // not stored on entity
        releaseYear = releaseYear?.toIntOrNull(),
        runtime = null,             // not stored on entity
        rating = voteAverage,       // TMDB vote average cached on entity
        genres = emptyList()        // not stored on entity
    ),
    note = note,
    addedAt = addedAt
)

// ─── Movie → WatchlistEntity ──────────────────────────────────────────────────

/**
 * Create a new [WatchlistEntity] from a domain [Movie].
 *
 * Called by [com.personal.cinevault.data.repository.WatchlistRepositoryImpl]
 * when the user adds a movie to their watchlist. The entity [id] is left as 0
 * so Room auto-generates it. [addedAt] is set to the current time.
 */
fun Movie.toWatchlistEntity(note: String? = null): WatchlistEntity = WatchlistEntity(
    id = 0,                                             // Room auto-generates PK
    tmdbMovieId = id,
    movieTitle = title,
    posterPath = posterPath,
    voteAverage = rating,                               // TMDB rating, not user rating
    releaseYear = releaseYear?.toString(),
    note = note,
    addedAt = System.currentTimeMillis()
)
