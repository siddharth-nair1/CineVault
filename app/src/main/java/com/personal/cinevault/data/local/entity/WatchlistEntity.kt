package com.personal.cinevault.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a movie the user has saved to their personal watchlist.
 *
 * The [tmdbMovieId] column has a unique index to prevent the same movie
 * from being added to the watchlist more than once.
 */
@Entity(
    tableName = "watchlist",
    indices = [Index(value = ["tmdbMovieId"], unique = true)]
)
data class WatchlistEntity(

    /** Auto-generated local primary key. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** TMDB movie ID. Enforced as unique — a movie can only be watchlisted once. */
    val tmdbMovieId: Int,

    /** Cached movie title at the time the entry was created. */
    val movieTitle: String,

    /** Cached poster path as returned by TMDB (e.g. "/abc123.jpg"). */
    val posterPath: String? = null,

    /** Cached TMDB vote average for display purposes. */
    val voteAverage: Float? = null,

    /** Release year of the movie (e.g. 2023), cached for display. */
    val releaseYear: String? = null,

    /** Optional user note about why this movie was added. */
    val note: String? = null,

    /**
     * Timestamp (epoch millis) when the movie was added to the watchlist.
     */
    val addedAt: Long = System.currentTimeMillis()
)
