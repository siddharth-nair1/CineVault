package com.personal.cinevault.domain.repository

import com.personal.cinevault.domain.model.Movie

/**
 * Repository contract for movie data — defined in the domain layer,
 * implemented in the data layer.
 */
interface MovieRepository {

    /** Search movies by a text query. */
    suspend fun searchMovies(query: String): Result<List<Movie>>

    /** Fetch full details for a single movie by its TMDB id. */
    suspend fun getMovieDetails(movieId: Int): Result<Movie>

    /** Get trending movies (defaults to weekly window). */
    suspend fun getTrending(): Result<List<Movie>>

    /** Get a page of currently popular movies. */
    suspend fun getPopular(page: Int = 1): Result<List<Movie>>
}
