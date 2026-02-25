package com.personal.cinevault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-created list (e.g. "My Favourites", "Watch Party Picks").
 *
 * Movies are associated with a list via [ListMovieEntity], which references
 * [id] as a foreign key.
 */
@Entity(tableName = "cine_lists")
data class CineListEntity(

    /** Auto-generated local primary key. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Display name of the list as entered by the user. */
    val name: String,

    /** Optional description for the list. */
    val description: String? = null,

    /**
     * Optional emoji or icon identifier for the list
     * (e.g. "🎬" or an icon resource name).
     */
    val icon: String? = null,

    /** Whether this list is pinned to the top of the lists screen. */
    val isPinned: Boolean = false,

    /** Display order among all lists (lower value = higher on screen). */
    val sortOrder: Int = 0,

    /**
     * Timestamp (epoch millis) when the list was created.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Timestamp (epoch millis) when the list or its movie set was last modified.
     */
    val updatedAt: Long = System.currentTimeMillis()
)
