package com.personal.cinevault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.cinevault.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [WatchlistEntity] / `watchlist` table.
 */
@Dao
interface WatchlistDao {

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Observe all watchlist entries, ordered by the time they were added
     * (newest first). Emits a new list whenever the table changes.
     */
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchlistEntity>>

    /**
     * Return the watchlist entry for the given movie, or `null` if it is
     * not in the watchlist. One-shot (suspend), not a Flow.
     */
    @Query("SELECT * FROM watchlist WHERE tmdbMovieId = :tmdbMovieId LIMIT 1")
    suspend fun getByMovieId(tmdbMovieId: Int): WatchlistEntity?

    /**
     * Observe whether a specific movie is currently in the watchlist.
     * Emits `true` when a matching row exists, `false` otherwise.
     * Useful for driving a toggle button in the UI reactively.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE tmdbMovieId = :tmdbMovieId)")
    fun isInWatchlist(tmdbMovieId: Int): Flow<Boolean>

    // ── Writes ───────────────────────────────────────────────────────────────

    /**
     * Add a movie to the watchlist.
     * Uses [OnConflictStrategy.IGNORE] because [WatchlistEntity.tmdbMovieId]
     * has a unique index — a duplicate insert is silently dropped rather than
     * causing an exception.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(watchlistEntry: WatchlistEntity)

    /**
     * Remove a specific watchlist entry from the table.
     */
    @Delete
    suspend fun delete(watchlistEntry: WatchlistEntity)
}
