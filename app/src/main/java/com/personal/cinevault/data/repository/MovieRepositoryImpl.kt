package com.personal.cinevault.data.repository

import com.personal.cinevault.data.local.MovieCacheManager
import com.personal.cinevault.data.remote.TmdbApiService
import com.personal.cinevault.data.remote.toDomain
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.MovieRepository

/**
 * Concrete implementation of [MovieRepository].
 *
 * Strategy:
 *  1. Check the [cache] for a stored result.
 *  2. On a cache miss, hit the [api] and write the result back to the cache.
 *  3. Any network/parsing exception is caught and returned as [Result.failure].
 */
class MovieRepositoryImpl(
    private val api: TmdbApiService,
    private val cache: MovieCacheManager
) : MovieRepository {

    // ─── Search ───────────────────────────────────────────────────────────────

    override suspend fun searchMovies(query: String): Result<List<Movie>> =
        runCatching {
            val cacheKey = "search:$query"
            cache.get(cacheKey) ?: run {
                val movies = api.searchMovies(query = query).results.map { it.toDomain() }
                cache.put(cacheKey, movies)
                movies
            }
        }

    // ─── Details ──────────────────────────────────────────────────────────────

    override suspend fun getMovieDetails(movieId: Int): Result<Movie> =
        runCatching {
            cache.getMovie(movieId) ?: run {
                val movie = api.getMovieDetails(movieId).toDomain()
                cache.putMovie(movie)
                movie
            }
        }

    // ─── Trending ─────────────────────────────────────────────────────────────

    override suspend fun getTrending(): Result<List<Movie>> =
        runCatching {
            val cacheKey = "trending:week"
            cache.get(cacheKey) ?: run {
                val movies = api.getTrending(timeWindow = "week").results.map { it.toDomain() }
                cache.put(cacheKey, movies)
                movies
            }
        }

    // ─── Popular ──────────────────────────────────────────────────────────────

    override suspend fun getPopular(page: Int): Result<List<Movie>> =
        runCatching {
            val cacheKey = "popular:$page"
            cache.get(cacheKey) ?: run {
                val movies = api.getPopular(page = page).results.map { it.toDomain() }
                cache.put(cacheKey, movies)
                movies
            }
        }
}
