package com.personal.cinevault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.cinevault.data.local.entity.CineListEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [CineListEntity] / `cine_lists` table.
 *
 * All reads are exposed as [Flow] so that the UI can react to changes
 * without manual refresh. Write functions are suspending and must be
 * called from a coroutine.
 */
@Dao
interface CineListDao {

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Observe all user-created lists, ordered by pinned status first, then
     * by [CineListEntity.sortOrder], then by most recently updated.
     * Emits a new list whenever the table changes.
     */
    @Query(
        """
        SELECT * FROM cine_lists
        ORDER BY isPinned DESC, sortOrder ASC, updatedAt DESC
        """
    )
    fun getAllLists(): Flow<List<CineListEntity>>

    /**
     * Return the list with the given [id], or `null` if it no longer exists.
     * This is a one-shot read (suspend, not Flow) — prefer [getAllLists] for
     * reactive UI and this for single fetches inside use cases.
     */
    @Query("SELECT * FROM cine_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: Int): CineListEntity?

    // ── Writes ───────────────────────────────────────────────────────────────

    /**
     * Persist a new list. Room assigns [CineListEntity.id] automatically
     * because [CineListEntity.id] is auto-generated.
     * Returns the newly inserted row ID.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertList(list: CineListEntity): Long

    /**
     * Replace every column of an existing list row matched by primary key.
     * Call this after mutating a [CineListEntity] copy (e.g. rename, reorder).
     */
    @Update
    suspend fun updateList(list: CineListEntity)

    /**
     * Delete the given list row. All associated [com.personal.cinevault.data.local.entity.ListMovieEntity]
     * rows are automatically removed via the CASCADE foreign-key rule.
     */
    @Delete
    suspend fun deleteList(list: CineListEntity)
}
