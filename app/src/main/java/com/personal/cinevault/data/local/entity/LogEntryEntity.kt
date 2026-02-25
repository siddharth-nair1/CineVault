package com.personal.cinevault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single watch-log entry recorded by the user.
 *
 * Each row captures one viewing event for a movie, including the user's
 * rating, whether they liked it, whether it was a rewatch, and the date
 * they watched it.
 */
@Entity(tableName = "log_entries")
data class LogEntryEntity(

    /** Auto-generated local primary key. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** TMDB movie ID — used to cross-reference remote data. */
    val tmdbMovieId: Int,

    /** Cached movie title at the time of logging. */
    val movieTitle: String,

    /** Cached poster path as returned by TMDB (e.g. "/abc123.jpg"). */
    val posterPath: String? = null,

    /** User rating out of 10 (e.g. 7.5). Null if not rated. */
    val rating: Float? = null,

    /** Whether the user marked this movie as "liked". */
    val liked: Boolean = false,

    /** Whether this viewing was a rewatch of a previously seen film. */
    val rewatch: Boolean = false,

    /**
     * The date the user watched the movie, stored as an ISO-8601 string
     * (e.g. "2024-06-15"). Null if the user did not specify a date.
     */
    val watchedDate: String? = null,

    /** Optional free-text notes the user added to this log entry. */
    val review: String? = null,

    /**
     * Timestamp (epoch millis) when this log entry was created on device.
     */
    val createdAt: Long = System.currentTimeMillis()
)
