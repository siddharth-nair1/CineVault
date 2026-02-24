package com.personal.cinevault.data.local

import com.personal.cinevault.domain.model.Movie

/**
 * Stub cache manager — always returns null (cache miss) for now.
 * Replace with a real Room-backed implementation when ready.
 */
class MovieCacheManager {

    /** Returns cached movies for [key], or null on a cache miss. */
    suspend fun get(key: String): List<Movie>? = null

    /** Saves [movies] under [key]. No-op in the stub. */
    suspend fun put(key: String, movies: List<Movie>) { /* TODO */ }

    /** Returns a cached movie by [id], or null on a cache miss. */
    suspend fun getMovie(id: Int): Movie? = null

    /** Saves a single [movie]. No-op in the stub. */
    suspend fun putMovie(movie: Movie) { /* TODO */ }
}
