package com.personal.cinevault.domain.model

/**
 * Domain model for a user-created movie list (e.g. "My Favourites").
 *
 * This is the object the UI and use-cases interact with. All Room-specific
 * annotations and concerns live in [com.personal.cinevault.data.local.entity.CineListEntity].
 *
 * Mapping between the entity and this model is done in `CineListMappers.kt`.
 */
data class CineList(

    /** Local database ID. 0 for a list that has not been persisted yet. */
    val id: Int = 0,

    /** Display name as entered by the user. */
    val name: String,

    /** Optional description for the list. */
    val description: String? = null,

    /**
     * Optional emoji or icon identifier (e.g. "🎬").
     * Displayed next to the list name in the UI.
     */
    val icon: String? = null,

    /** Whether the list is pinned to the top of the lists screen. */
    val isPinned: Boolean = false,

    /** Sort position among all lists (lower = appears higher). */
    val sortOrder: Int = 0,

    /** Epoch-millis timestamp of when the list was created. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Epoch-millis timestamp of the last modification. */
    val updatedAt: Long = System.currentTimeMillis(),
)
