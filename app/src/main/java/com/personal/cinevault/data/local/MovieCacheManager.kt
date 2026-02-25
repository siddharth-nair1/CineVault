package com.personal.cinevault.data.local

import com.personal.cinevault.data.local.dao.MovieCacheDao
import com.personal.cinevault.data.local.entity.MovieCacheEntity

/**
 * High-level cache manager that wraps [MovieCacheDao] with size-bounding and
 * TTL eviction logic.
 *
 * **Cache strategy:**
 * 1. **LRU eviction** — once the entry count reaches [EVICT_THRESHOLD] (550),
 *    the table is trimmed down to [MAX_ENTRIES] (500) by removing the
 *    least-recently-accessed rows.
 * 2. **TTL eviction** — [runMaintenance] deletes rows whose [cachedAt]
 *    timestamp is older than 7 days (mirrors [MovieCacheEntity.isStale]).
 * 3. **Access tracking** — [get] calls [MovieCacheDao.updateLastAccessed] on
 *    every cache hit so LRU ordering stays accurate.
 *
 * @param dao The [MovieCacheDao] used for all database operations.
 */
class MovieCacheManager(private val dao: MovieCacheDao) {

    companion object {
        /** Maximum number of entries to retain after LRU eviction. */
        const val MAX_ENTRIES = 500

        /**
         * Entry count at which LRU eviction is triggered inside [put].
         * Set slightly above [MAX_ENTRIES] to batch eviction work and avoid
         * pruning on every insert.
         */
        const val EVICT_THRESHOLD = 550

        /** 7 days in milliseconds — matches [MovieCacheEntity.isStale]. */
        private const val STALE_THRESHOLD_MS = 7L * 24 * 60 * 60 * 1_000
    }

    /**
     * Return the cached [MovieCacheEntity] for [movieId], or `null` on a miss.
     *
     * On a hit, bumps [MovieCacheEntity.lastAccessedAt] so the LRU ordering
     * stays current. The caller is responsible for checking
     * [MovieCacheEntity.isStale] and re-fetching from the network if needed.
     */
    suspend fun get(movieId: Int): MovieCacheEntity? {
        val entry = dao.getById(movieId) ?: return null
        dao.updateLastAccessed(movieId)
        return entry
    }

    /**
     * Insert or replace [entity] in the cache.
     *
     * After inserting, checks whether the table has grown beyond
     * [EVICT_THRESHOLD]. If so, triggers LRU eviction to trim back down to
     * [MAX_ENTRIES] rows. Eviction is performed in the same coroutine context
     * as the insert so callers don't need to manage it separately.
     */
    suspend fun put(entity: MovieCacheEntity) {
        dao.insert(entity)
        if (dao.getCount() >= EVICT_THRESHOLD) {
            dao.evictOldEntries(keepCount = MAX_ENTRIES)
        }
    }

    /**
     * Perform full cache maintenance. Intended to be called periodically from
     * a [androidx.work.WorkManager] task or on app foreground:
     *
     * 1. Delete all entries older than 7 days ([MovieCacheEntity.isStale]).
     * 2. If the cache is still over [MAX_ENTRIES] after TTL eviction, trim it
     *    down with LRU eviction.
     */
    suspend fun runMaintenance() {
        // Step 1 — TTL eviction: remove stale entries.
        val staleBeforeMs = System.currentTimeMillis() - STALE_THRESHOLD_MS
        dao.deleteStaleEntries(staleBeforeMs)

        // Step 2 — LRU eviction: clamp size in case many non-stale entries
        // accumulated (e.g. user ran offline for < 7 days with heavy browsing).
        if (dao.getCount() > MAX_ENTRIES) {
            dao.evictOldEntries(keepCount = MAX_ENTRIES)
        }
    }
}
