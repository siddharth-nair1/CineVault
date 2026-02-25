package com.personal.cinevault.domain.repository

import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for the user's personal watchlist — defined in the
 * domain layer, implemented in the data layer by
 * [com.personal.cinevault.data.repository.WatchlistRepositoryImpl].
 */
interface WatchlistRepository {

    /**
     * Observe all watchlist entries as a reactive stream, ordered by most
     * recently added first. Emits a new list whenever the table changes.
     */
    fun getAllWatchlist(): Flow<List<WatchlistItem>>

    /**
     * Add a [movie] to the watchlist. If the movie is already present, the
     * operation is a no-op (the DAO uses IGNORE conflict strategy).
     */
    suspend fun addToWatchlist(movie: Movie)

    /**
     * Remove the movie with [tmdbMovieId] from the watchlist.
     * Does nothing if the movie is not in the watchlist.
     */
    suspend fun removeFromWatchlist(tmdbMovieId: Int)

    /**
     * Observe whether the movie with [tmdbMovieId] is currently in the
     * watchlist. Emits `true`/`false` reactively as the table changes.
     */
    fun isInWatchlist(tmdbMovieId: Int): Flow<Boolean>
}
