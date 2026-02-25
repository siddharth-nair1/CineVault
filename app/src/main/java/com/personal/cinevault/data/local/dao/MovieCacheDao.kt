package com.personal.cinevault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.cinevault.data.local.entity.MovieCacheEntity

/**
 * Data Access Object for the [MovieCacheEntity] / `movie_cache` table.
 *
 * Cache management strategy:
 *  - **LRU eviction** via [evictOldEntries]: keeps only the 500 most recently
 *    accessed rows; excess rows are deleted by oldest [lastAccessedAt] first.
 *  - **TTL eviction** via [deleteStaleEntries]: removes any row whose
 *    [cachedAt] timestamp is older than the supplied threshold (7 days).
 *  - **Access tracking** via [updateLastAccessed]: called on every cache hit
 *    so LRU ordering stays accurate.
 */
@Dao
interface MovieCacheDao {

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Return the cached entry for the given TMDB movie ID, or `null` on a
     * cache miss. One-shot suspend function — callers should then check
     * [MovieCacheEntity.isStale] before using the result.
     */
    @Query("SELECT * FROM movie_cache WHERE tmdbMovieId = :tmdbMovieId LIMIT 1")
    suspend fun getById(tmdbMovieId: Int): MovieCacheEntity?

    /**
     * Return the total number of rows currently in the cache table.
     * Useful for monitoring cache size without loading all rows.
     */
    @Query("SELECT COUNT(*) FROM movie_cache")
    suspend fun getCount(): Int

    // ── Writes ───────────────────────────────────────────────────────────────

    /**
     * Insert or replace a cached movie entry.
     * [OnConflictStrategy.REPLACE] ensures the JSON and timestamps are
     * refreshed when the same [MovieCacheEntity.tmdbMovieId] is re-fetched.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cacheEntry: MovieCacheEntity)

    /**
     * Bump the [lastAccessedAt] timestamp for the given movie to now.
     * Must be called after every successful cache hit to keep the LRU
     * ordering accurate.
     */
    @Query(
        "UPDATE movie_cache SET lastAccessedAt = :accessedAt WHERE tmdbMovieId = :tmdbMovieId"
    )
    suspend fun updateLastAccessed(tmdbMovieId: Int, accessedAt: Long = System.currentTimeMillis())

    /**
     * LRU eviction: delete all rows except the [keepCount] most recently
     * accessed ones. The default limit of 500 keeps memory usage bounded.
     *
     * Rows are ordered by [lastAccessedAt] ascending so the oldest-accessed
     * (least recently used) entries are removed first.
     */
    @Query(
        """
        DELETE FROM movie_cache
        WHERE tmdbMovieId NOT IN (
            SELECT tmdbMovieId
            FROM movie_cache
            ORDER BY lastAccessedAt DESC
            LIMIT :keepCount
        )
        """
    )
    suspend fun evictOldEntries(keepCount: Int = 500)

    /**
     * TTL eviction: delete all rows whose [cachedAt] timestamp is older than
     * [staleBeforeMs]. Pass `System.currentTimeMillis() - 7_days_in_ms` to
     * remove anything cached more than 7 days ago.
     */
    @Query("DELETE FROM movie_cache WHERE cachedAt < :staleBeforeMs")
    suspend fun deleteStaleEntries(staleBeforeMs: Long)
}
