package com.personal.cinevault.data.import

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for TMDB searches to avoid repeating API calls for the same movie
 * during an import.
 *
 * Keys should be formatted as "Title_Year", e.g. "The Matrix_1999".
 * Values are the TMDB movie ID, or null if the API returned no results.
 */
class TmdbSearchCache {
    private val cache = ConcurrentHashMap<String, Int?>()

    fun get(key: String): Int? {
        // We use map contains so we can distinguish between 
        // "not fetched yet" and "fetched but returned null"
        if (!cache.containsKey(key)) {
            // Return a special marker or just rely on containsKey in the caller.
            // A more robust way: wrap the result.
        }
        return cache[key]
    }
    
    fun contains(key: String): Boolean {
        return cache.containsKey(key)
    }

    fun put(key: String, tmdbId: Int?) {
        cache[key] = tmdbId
    }
    
    fun clear() {
        cache.clear()
    }
}
