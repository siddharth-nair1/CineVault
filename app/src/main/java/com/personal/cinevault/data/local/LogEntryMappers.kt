package com.personal.cinevault.data.local

import com.personal.cinevault.data.local.entity.LogEntryEntity
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.model.Movie

// ─── LogEntryEntity → LogEntry ────────────────────────────────────────────────

/**
 * Map a database [LogEntryEntity] to the clean domain [LogEntry].
 *
 * The entity stores only a minimal snapshot of the movie (id, title,
 * posterPath) to avoid duplicating full movie data. The resulting [Movie]
 * will have null for fields not cached on the entity (overview, backdrop,
 * releaseYear, runtime, rating, genres). Callers that need the full movie
 * should enrich the result using [MovieCacheManager] or the network.
 */
fun LogEntryEntity.toDomain(): LogEntry = LogEntry(
    id = id,
    movie = Movie(
        id = tmdbMovieId,
        title = movieTitle,
        overview = "",          // not stored on entity — enrich from cache if needed
        posterPath = posterPath,
        backdropPath = null,    // not stored on entity
        releaseYear = null,     // not stored on entity
        runtime = null,         // not stored on entity
        rating = null,          // entity rating is the user's rating, not TMDB's
        genres = emptyList()    // not stored on entity
    ),
    rating = rating,
    liked = liked,
    rewatch = rewatch,
    review = review,
    watchedDate = watchedDate,
    createdAt = createdAt
)

// ─── LogEntry → LogEntryEntity ────────────────────────────────────────────────

/**
 * Map a domain [LogEntry] to the Room [LogEntryEntity] ready for persistence.
 *
 * Only the fields that are available on the entity are written. Full movie
 * detail is NOT stored on the entity — only the minimal snapshot needed to
 * display the entry without a network call.
 */
fun LogEntry.toEntity(): LogEntryEntity = LogEntryEntity(
    id = id,
    tmdbMovieId = movie.id,
    movieTitle = movie.title,
    posterPath = movie.posterPath,
    rating = rating,
    liked = liked,
    rewatch = rewatch,
    review = review,
    watchedDate = watchedDate,
    createdAt = createdAt
)
