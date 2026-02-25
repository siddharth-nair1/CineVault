package com.personal.cinevault.data.local

import com.personal.cinevault.data.local.entity.CineListEntity
import com.personal.cinevault.data.local.entity.ListMovieEntity
import com.personal.cinevault.domain.model.CineList
import com.personal.cinevault.domain.model.ListMovie

// ── CineListEntity ↔ CineList ────────────────────────────────────────────────

/**
 * Map a [CineListEntity] (Room) to the domain [CineList].
 */
fun CineListEntity.toDomain(): CineList = CineList(
    id          = id,
    name        = name,
    description = description,
    icon        = icon,
    isPinned    = isPinned,
    sortOrder   = sortOrder,
    createdAt   = createdAt,
    updatedAt   = updatedAt,
)

/**
 * Map a domain [CineList] to [CineListEntity] for persistence.
 *
 * [CineList.id] defaults to 0; Room will auto-generate a real ID on first
 * insert.
 */
fun CineList.toEntity(): CineListEntity = CineListEntity(
    id          = id,
    name        = name,
    description = description,
    icon        = icon,
    isPinned    = isPinned,
    sortOrder   = sortOrder,
    createdAt   = createdAt,
    updatedAt   = updatedAt,
)

// ── ListMovieEntity ↔ ListMovie ───────────────────────────────────────────────

/**
 * Map a [ListMovieEntity] (Room) to the domain [ListMovie].
 */
fun ListMovieEntity.toDomain(): ListMovie = ListMovie(
    listId       = listId,
    tmdbMovieId  = tmdbMovieId,
    movieTitle   = movieTitle,
    posterPath   = posterPath,
    voteAverage  = voteAverage,
    releaseYear  = releaseYear,
    sortOrder    = sortOrder,
    addedAt      = addedAt,
)

/**
 * Map a domain [ListMovie] to [ListMovieEntity] for persistence.
 */
fun ListMovie.toEntity(): ListMovieEntity = ListMovieEntity(
    listId       = listId,
    tmdbMovieId  = tmdbMovieId,
    movieTitle   = movieTitle,
    posterPath   = posterPath,
    voteAverage  = voteAverage,
    releaseYear  = releaseYear,
    sortOrder    = sortOrder,
    addedAt      = addedAt,
)
