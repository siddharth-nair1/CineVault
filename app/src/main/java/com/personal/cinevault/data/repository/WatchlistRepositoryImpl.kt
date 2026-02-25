package com.personal.cinevault.data.repository

import com.personal.cinevault.data.local.dao.WatchlistDao
import com.personal.cinevault.data.local.toDomain
import com.personal.cinevault.data.local.toWatchlistEntity
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.model.WatchlistItem
import com.personal.cinevault.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [WatchlistRepository] backed by Room via
 * [WatchlistDao].
 *
 * Mapping between [WatchlistEntity] and [WatchlistItem] / [Movie] is
 * delegated to the extension functions in `WatchlistMappers.kt`.
 */
class WatchlistRepositoryImpl(
    private val dao: WatchlistDao
) : WatchlistRepository {

    /**
     * Observe all watchlist entries as a reactive [Flow], mapping each entity
     * to the domain [WatchlistItem]. Emits a new list on every table change.
     */
    override fun getAllWatchlist(): Flow<List<WatchlistItem>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Add [movie] to the watchlist by converting it to a [WatchlistEntity].
     * The DAO ignores duplicates (unique index on tmdbMovieId, IGNORE conflict
     * strategy), so calling this multiple times for the same movie is safe.
     */
    override suspend fun addToWatchlist(movie: Movie) =
        dao.insert(movie.toWatchlistEntity())

    /**
     * Remove the movie with [tmdbMovieId] from the watchlist.
     * Looks up the existing entity first; does nothing if not found.
     */
    override suspend fun removeFromWatchlist(tmdbMovieId: Int) {
        val entity = dao.getByMovieId(tmdbMovieId) ?: return
        dao.delete(entity)
    }

    /**
     * Observe whether the movie with [tmdbMovieId] is in the watchlist.
     * Emits `true`/`false` reactively — ideal for driving a bookmark/add
     * toggle button in the UI.
     */
    override fun isInWatchlist(tmdbMovieId: Int): Flow<Boolean> =
        dao.isInWatchlist(tmdbMovieId)
}
