package com.personal.cinevault.domain.model

/**
 * Clean domain model representing a movie saved to the user's watchlist.
 *
 * All fields are framework-free. Entity ↔ domain mapping lives in
 * `data/local/WatchlistMappers.kt`.
 */
data class WatchlistItem(
    /** Local database primary key (0 for unsaved entries). */
    val id: Int = 0,

    /** The movie saved to the watchlist. Populated from cached fields on the
     *  entity; full detail may be absent until enriched from the network. */
    val movie: Movie,

    /** Optional note the user attached when adding the movie. */
    val note: String? = null,

    /** Epoch-millis timestamp of when the movie was added to the watchlist. */
    val addedAt: Long = System.currentTimeMillis()
)
