package com.personal.cinevault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.personal.cinevault.data.local.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [ReviewEntity] / `reviews` table.
 */
@Dao
interface ReviewDao {

    // ── Reads ────────────────────────────────────────────────────────────────

    /**
     * Observe all reviews, ordered by most recently updated first.
     * Emits a new list whenever the underlying table changes.
     */
    @Query("SELECT * FROM reviews ORDER BY updatedAt DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    /**
     * Observe all reviews written for a specific movie.
     * Emits a new list on every change.
     */
    @Query("SELECT * FROM reviews WHERE tmdbMovieId = :tmdbMovieId ORDER BY updatedAt DESC")
    fun getReviewsForMovie(tmdbMovieId: Int): Flow<List<ReviewEntity>>

    /**
     * Load a single review by its primary key. Returns null if not found.
     * One-shot suspend call — not reactive.
     */
    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ReviewEntity?

    // ── Writes ───────────────────────────────────────────────────────────────

    /**
     * Insert or replace a review.
     * If a row with the same primary key already exists it is replaced in full.
     */
    @Upsert
    suspend fun upsert(review: ReviewEntity)

    /**
     * Update an existing review (partial-field update friendly).
     * The [ReviewEntity.id] must match an existing row.
     */
    @Update
    suspend fun update(review: ReviewEntity)

    /**
     * Delete a specific review.
     */
    @Delete
    suspend fun delete(review: ReviewEntity)
}
