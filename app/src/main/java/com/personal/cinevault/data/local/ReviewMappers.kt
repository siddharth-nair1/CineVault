package com.personal.cinevault.data.local

import com.personal.cinevault.data.local.entity.ReviewEntity
import com.personal.cinevault.domain.model.Review

// ─── ReviewEntity → Review ────────────────────────────────────────────────────

/**
 * Map a database [ReviewEntity] to the clean domain [Review].
 */
fun ReviewEntity.toDomain(): Review = Review(
    id               = id,
    tmdbMovieId      = tmdbMovieId,
    movieTitle       = movieTitle,
    posterPath       = posterPath,
    content          = reviewText,
    rating           = rating,
    containsSpoilers = containsSpoilers,
    createdAt        = createdAt,
)

// ─── Review → ReviewEntity ────────────────────────────────────────────────────

/**
 * Map a domain [Review] back to the Room [ReviewEntity] ready for persistence.
 */
fun Review.toEntity(): ReviewEntity = ReviewEntity(
    id               = id,
    tmdbMovieId      = tmdbMovieId,
    movieTitle       = movieTitle,
    posterPath       = posterPath,
    reviewText       = content,
    rating           = rating,
    containsSpoilers = containsSpoilers,
    createdAt        = createdAt,
    updatedAt        = System.currentTimeMillis(),
)
