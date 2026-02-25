package com.personal.cinevault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.cinevault.data.local.entity.ListMovieEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [ListMovieEntity] / `list_movies` join table.
 *
 * Scoped to a single list by [listId]. All reads are [Flow]-based for
 * reactive UI updates. Write operations are suspending coroutine functions.
 */
@Dao
interface ListMovieDao {

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Observe all movies that belong to [listId], ordered by their display
     * position ([ListMovieEntity.sortOrder] ascending), then by most recently
     * added as a tiebreaker.
     *
     * Emits a new list whenever any row in the `list_movies` table changes.
     */
    @Query(
        """
        SELECT * FROM list_movies
        WHERE listId = :listId
        ORDER BY sortOrder ASC, addedAt DESC
        """
    )
    fun getMoviesInList(listId: Int): Flow<List<ListMovieEntity>>

    /**
     * Check whether [tmdbMovieId] is already present in [listId].
     * Emits `true`/`false` reactively — drives add/remove toggle buttons.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM list_movies
            WHERE listId = :listId AND tmdbMovieId = :tmdbMovieId
        )
        """
    )
    fun isMovieInList(listId: Int, tmdbMovieId: Int): Flow<Boolean>

    // ── Writes ───────────────────────────────────────────────────────────────

    /**
     * Add a movie to a list. Uses [OnConflictStrategy.IGNORE] so that
     * inserting the same (listId, tmdbMovieId) pair a second time is a no-op
     * — the composite primary key enforces uniqueness.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMovieToList(item: ListMovieEntity)

    /**
     * Remove the movie identified by [tmdbMovieId] from the list identified
     * by [listId]. Does nothing if the movie is not in the list.
     */
    @Query("DELETE FROM list_movies WHERE listId = :listId AND tmdbMovieId = :tmdbMovieId")
    suspend fun removeMovieFromList(listId: Int, tmdbMovieId: Int)

    /**
     * Update the display [position] of [tmdbMovieId] within [listId].
     * Used when the user reorders movies inside a list.
     */
    @Query(
        """
        UPDATE list_movies
        SET sortOrder = :position
        WHERE listId = :listId AND tmdbMovieId = :tmdbMovieId
        """
    )
    suspend fun updatePosition(listId: Int, tmdbMovieId: Int, position: Int)
}
