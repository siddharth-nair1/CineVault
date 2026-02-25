package com.personal.cinevault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a written review the user has composed for a movie.
 *
 * A review is distinct from a log entry: a user may write a detailed
 * review independently of (or in addition to) logging a watch event.
 */
@Entity(tableName = "reviews")
data class ReviewEntity(

    /** Auto-generated local primary key. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** TMDB movie ID — used to cross-reference remote data. */
    val tmdbMovieId: Int,

    /** Cached movie title at the time the review was written. */
    val movieTitle: String,

    /** Cached poster path as returned by TMDB (e.g. "/abc123.jpg"). */
    val posterPath: String? = null,

    /** User rating out of 10 (e.g. 8.0). Null if the review has no rating. */
    val rating: Float? = null,

    /** Whether the user marked this movie as "liked". */
    val liked: Boolean = false,

    /** The body text of the review. */
    val reviewText: String,

    /** Whether the review contains spoilers. */
    val containsSpoilers: Boolean = false,

    /**
     * Timestamp (epoch millis) when this review was first created on device.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Timestamp (epoch millis) when this review was last edited.
     * Initially the same as [createdAt].
     */
    val updatedAt: Long = System.currentTimeMillis()
)
