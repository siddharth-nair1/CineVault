package com.personal.cinevault.data.repository

import com.personal.cinevault.data.local.dao.CineListDao
import com.personal.cinevault.data.local.dao.ListMovieDao
import com.personal.cinevault.data.local.toDomain
import com.personal.cinevault.data.local.toEntity
import com.personal.cinevault.domain.model.CineList
import com.personal.cinevault.domain.model.ListMovie
import com.personal.cinevault.domain.repository.CineListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [CineListRepository] backed by Room via
 * [CineListDao] and [ListMovieDao].
 *
 * Entity-to-domain mapping is delegated to the extension functions in
 * `CineListMappers.kt`.
 */
class CineListRepositoryImpl(
    private val listDao: CineListDao,
    private val listMovieDao: ListMovieDao,
) : CineListRepository {

    // ── Lists ─────────────────────────────────────────────────────────────────

    /**
     * Observe all lists as a reactive domain [Flow].
     * Pinned lists appear first, then sorted by [CineList.sortOrder].
     */
    override fun getAllLists(): Flow<List<CineList>> =
        listDao.getAllLists().map { entities -> entities.map { it.toDomain() } }

    /**
     * Fetch a single list by [id]. Returns `null` if the list has been deleted.
     */
    override suspend fun getListById(id: Int): CineList? =
        listDao.getListById(id)?.toDomain()

    /**
     * Create a new list. The [CineList.id] in the returned value reflects the
     * auto-generated row ID assigned by Room.
     */
    override suspend fun createList(list: CineList): Long =
        listDao.insertList(list.toEntity())

    /**
     * Persist changes to an existing list.
     */
    override suspend fun updateList(list: CineList) =
        listDao.updateList(list.toEntity())

    /**
     * Delete a list and all its movies (cascade handled by Room FK).
     */
    override suspend fun deleteList(list: CineList) =
        listDao.deleteList(list.toEntity())

    // ── Movies inside a list ──────────────────────────────────────────────────

    /**
     * Observe all movies in [listId] as a reactive domain [Flow].
     */
    override fun getMoviesInList(listId: Int): Flow<List<ListMovie>> =
        listMovieDao.getMoviesInList(listId).map { entities -> entities.map { it.toDomain() } }

    /**
     * Observe whether [tmdbMovieId] is in [listId].
     * Emits `true`/`false` reactively — suitable for driving toggle buttons.
     */
    override fun isMovieInList(listId: Int, tmdbMovieId: Int): Flow<Boolean> =
        listMovieDao.isMovieInList(listId, tmdbMovieId)

    /**
     * Add [movie] to its designated list. Duplicate inserts are silently
     * ignored by the DAO's IGNORE conflict strategy.
     */
    override suspend fun addMovieToList(movie: ListMovie) =
        listMovieDao.addMovieToList(movie.toEntity())

    /**
     * Remove the movie with [tmdbMovieId] from the list with [listId].
     */
    override suspend fun removeMovieFromList(listId: Int, tmdbMovieId: Int) =
        listMovieDao.removeMovieFromList(listId, tmdbMovieId)

    /**
     * Update the display [position] of [tmdbMovieId] inside [listId].
     */
    override suspend fun updateMoviePosition(listId: Int, tmdbMovieId: Int, position: Int) =
        listMovieDao.updatePosition(listId, tmdbMovieId, position)
}
