package com.personal.cinevault.domain.model

/**
 * Clean domain model representing a user's diary/log entry for a watched film.
 *
 * All fields are framework-free. Entity ↔ domain mapping lives in
 * `data/local/LogEntryMappers.kt`.
 */
data class LogEntry(
    /** Local database primary key (0 for unsaved entries). */
    val id: Int = 0,

    /** The movie that was watched. Populated from cached fields on the entity;
     *  full detail (overview, backdrop, genres) may be absent until enriched
     *  from the network. */
    val movie: Movie,

    /** User's numerical rating out of 10, or null if unrated. */
    val rating: Float? = null,

    /** Whether the user marked this entry as "liked". */
    val liked: Boolean = false,

    /** Whether this viewing was a rewatch of a previously seen film. */
    val rewatch: Boolean = false,

    /** Optional free-text note or mini-review for this viewing. */
    val review: String? = null,

    /** The date the user watched the movie, as an ISO-8601 string ("2024-06-15"),
     *  or null if not specified. Stored as String to avoid java.time serialisation
     *  complexity across layers. */
    val watchedDate: String? = null,

    /** Epoch-millis timestamp of when this entry was created on device. */
    val createdAt: Long = System.currentTimeMillis()
)
