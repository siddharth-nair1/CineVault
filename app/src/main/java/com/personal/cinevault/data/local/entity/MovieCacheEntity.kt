package com.personal.cinevault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Number of milliseconds in 7 days — used for staleness checks. */
private const val STALE_THRESHOLD_MS = 7L * 24 * 60 * 60 * 1_000

/**
 * Cached representation of a TMDB movie, stored as a serialised JSON string.
 *
 * Entries are considered stale after [STALE_THRESHOLD_MS] (7 days) and will
 * be evicted by [MovieCacheDao.deleteStaleEntries] or the LRU eviction in
 * [MovieCacheDao.evictOldEntries].
 */
@Entity(tableName = "movie_cache")
data class MovieCacheEntity(

    /**
     * TMDB movie ID — used as the primary key so each movie has at most one
     * cached entry.
     */
    @PrimaryKey
    val tmdbMovieId: Int,

    /**
     * Full movie detail JSON as returned by TMDB
     * (e.g. the /movie/{id} response body serialised to a String).
     */
    val json: String,

    /** Timestamp (epoch millis) when this entry was first inserted. */
    val cachedAt: Long = System.currentTimeMillis(),

    /**
     * Timestamp (epoch millis) of the most recent read for this entry.
     * Updated by [MovieCacheDao.updateLastAccessed] on every cache hit and
     * used by the LRU eviction query to identify least-recently-used rows.
     */
    val lastAccessedAt: Long = System.currentTimeMillis()
) {

    /**
     * Returns `true` when this cache entry is older than 7 days and should
     * be refreshed from the network.
     */
    fun isStale(): Boolean =
        System.currentTimeMillis() - cachedAt > STALE_THRESHOLD_MS
}
