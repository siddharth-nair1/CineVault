package com.personal.cinevault.data.repository

import com.google.gson.Gson
import com.personal.cinevault.data.local.MovieCacheManager
import com.personal.cinevault.data.local.entity.MovieCacheEntity
import com.personal.cinevault.data.remote.TmdbApiService
import com.personal.cinevault.data.remote.dto.MovieDetailResponse
import com.personal.cinevault.data.remote.toDomain
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.MovieRepository

/**
 * Concrete implementation of [MovieRepository].
 *
 * Cache strategy:
 *  - **Movie details** (`getMovieDetails`): cached as serialised [MovieDetailResponse] JSON
 *    inside [MovieCacheEntity], keyed by TMDB movie ID. A stale entry is
 *    silently refreshed from the network.
 *  - **List results** (search, trending, popular): not cached at the entity level —
 *    served directly from the network. Individual movies that appear in list
 *    results can be cached on demand from their detail call.
 */
class MovieRepositoryImpl(
    private val api: TmdbApiService,
    private val cache: MovieCacheManager,
    private val gson: Gson
) : MovieRepository {

    // ─── Search ───────────────────────────────────────────────────────────────

    override suspend fun searchMovies(query: String): Result<List<Movie>> =
        runCatching {
            api.searchMovies(query = query).results.map { it.toDomain() }
        }

    // ─── Details ──────────────────────────────────────────────────────────────

    override suspend fun getMovieDetails(movieId: Int): Result<Movie> =
        runCatching {
            // 1. Try cache hit — deserialise stored JSON back to the DTO.
            val cached = cache.get(movieId)
            if (cached != null && !cached.isStale()) {
                return@runCatching gson
                    .fromJson(cached.json, MovieDetailResponse::class.java)
                    .toDomain()
            }

            // 2. Cache miss or stale — fetch from network.
            val response = api.getMovieDetails(movieId)

            // 3. Write fresh entry to cache.
            cache.put(
                MovieCacheEntity(
                    tmdbMovieId = response.id,
                    json = gson.toJson(response),
                    cachedAt = System.currentTimeMillis(),
                    lastAccessedAt = System.currentTimeMillis()
                )
            )

            response.toDomain()
        }

    // ─── Trending ─────────────────────────────────────────────────────────────

    override suspend fun getTrending(): Result<List<Movie>> =
        runCatching {
            api.getTrending(timeWindow = "week").results.map { it.toDomain() }
        }

    // ─── Popular ──────────────────────────────────────────────────────────────

    override suspend fun getPopular(page: Int): Result<List<Movie>> =
        runCatching {
            api.getPopular(page = page).results.map { it.toDomain() }
        }
}
