package com.personal.cinevault.data.remote

import com.personal.cinevault.data.remote.dto.MovieDetailResponse
import com.personal.cinevault.data.remote.dto.MovieDto
import com.personal.cinevault.domain.model.Movie

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun String?.toReleaseYear(): Int? =
    this?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()

// ─── MovieDto → Movie ─────────────────────────────────────────────────────────

/**
 * Maps a summary DTO (used in search/list results) to the domain [Movie].
 * Genre names are not available in list responses (only IDs), so [genres] is
 * left empty — callers should enrich via a genre lookup if needed.
 */
fun MovieDto.toDomain(): Movie = Movie(
    id          = id,
    title       = title,
    overview    = overview,
    posterPath  = posterPath,
    backdropPath = backdropPath,
    releaseYear = releaseDate.toReleaseYear(),
    runtime     = null,            // not included in list/search responses
    rating      = voteAverage.toFloat().takeIf { it > 0f },
    genres      = emptyList()      // genre_ids only — resolve separately if needed
)

// ─── MovieDetailResponse → Movie ──────────────────────────────────────────────

/**
 * Maps a full detail response (single movie endpoint) to the domain [Movie].
 * Includes runtime and fully resolved genre names.
 */
fun MovieDetailResponse.toDomain(): Movie = Movie(
    id          = id,
    title       = title,
    overview    = overview,
    posterPath  = posterPath,
    backdropPath = backdropPath,
    releaseYear = releaseDate.toReleaseYear(),
    runtime     = runtime,
    rating      = voteAverage.toFloat().takeIf { it > 0f },
    genres      = genres.map { it.name }
)
