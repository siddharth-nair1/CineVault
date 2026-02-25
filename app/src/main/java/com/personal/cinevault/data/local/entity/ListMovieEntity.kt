package com.personal.cinevault.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Join table that maps movies to user-created lists.
 *
 * Uses a composite primary key of ([listId], [tmdbMovieId]) so that the
 * same movie can appear in multiple different lists, but cannot appear
 * in the same list more than once.
 *
 * A cascade-delete foreign key on [listId] means all movie associations
 * are removed automatically when their parent [CineListEntity] is deleted.
 */
@Entity(
    tableName = "list_movies",
    primaryKeys = ["listId", "tmdbMovieId"],
    foreignKeys = [
        ForeignKey(
            entity = CineListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class ListMovieEntity(

    /** References [CineListEntity.id] — the list this movie belongs to. */
    val listId: Int,

    /** TMDB movie ID — part of the composite primary key. */
    val tmdbMovieId: Int,

    /** Cached movie title at the time the movie was added to the list. */
    val movieTitle: String,

    /** Cached poster path as returned by TMDB (e.g. "/abc123.jpg"). */
    val posterPath: String? = null,

    /** Cached TMDB vote average for display purposes. */
    val voteAverage: Float? = null,

    /** Release year of the movie (e.g. "2023"), cached for display. */
    val releaseYear: String? = null,

    /** Display order of this movie within the list (lower = higher on screen). */
    val sortOrder: Int = 0,

    /**
     * Timestamp (epoch millis) when the movie was added to this list.
     */
    val addedAt: Long = System.currentTimeMillis()
)
