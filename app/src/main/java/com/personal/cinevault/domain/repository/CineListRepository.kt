package com.personal.cinevault.domain.repository

import com.personal.cinevault.domain.model.CineList
import com.personal.cinevault.domain.model.ListMovie
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user-created movie lists — defined in the domain
 * layer, implemented in the data layer by
 * [com.personal.cinevault.data.repository.CineListRepositoryImpl].
 */
interface CineListRepository {

    // ── Lists ─────────────────────────────────────────────────────────────────

    /**
     * Observe all user-created lists as a reactive stream, ordered by pinned
     * status then sort order. Emits a new list on every table change.
     */
    fun getAllLists(): Flow<List<CineList>>

    /**
     * Fetch a single list by [id]. Returns `null` if it has been deleted.
     * One-shot suspend call — not reactive.
     */
    suspend fun getListById(id: Int): CineList?

    /**
     * Persist a new [list]. Room auto-generates its ID.
     * Returns the ID of the newly created row.
     */
    suspend fun createList(list: CineList): Long

    /**
     * Persist changes to an existing [list] (name, description, icon, pin,
     * sort order, etc.).
     */
    suspend fun updateList(list: CineList)

    /**
     * Delete [list] and all its associated movie entries (cascade).
     */
    suspend fun deleteList(list: CineList)

    // ── Movies inside a list ──────────────────────────────────────────────────

    /**
     * Observe all movies belonging to the list with [listId],
     * ordered by sort position. Emits on every table change.
     */
    fun getMoviesInList(listId: Int): Flow<List<ListMovie>>

    /**
     * Observe whether [tmdbMovieId] is currently in the list [listId].
     * Emits `true`/`false` reactively — drives add/remove toggle buttons.
     */
    fun isMovieInList(listId: Int, tmdbMovieId: Int): Flow<Boolean>

    /**
     * Add [movie] to the list. If the movie is already present the call is
     * a no-op (IGNORE conflict strategy on the DAO).
     */
    suspend fun addMovieToList(movie: ListMovie)

    /**
     * Remove the movie with [tmdbMovieId] from the list with [listId].
     */
    suspend fun removeMovieFromList(listId: Int, tmdbMovieId: Int)

    /**
     * Update the display [position] of [tmdbMovieId] inside [listId].
     * Used for drag-to-reorder inside a list.
     */
    suspend fun updateMoviePosition(listId: Int, tmdbMovieId: Int, position: Int)
}
