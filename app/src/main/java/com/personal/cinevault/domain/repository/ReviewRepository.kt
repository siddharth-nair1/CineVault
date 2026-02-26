package com.personal.cinevault.domain.repository

import com.personal.cinevault.domain.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user-written movie reviews — defined in the domain
 * layer, implemented in the data layer by
 * [com.personal.cinevault.data.repository.ReviewRepositoryImpl].
 */
interface ReviewRepository {

    /**
     * Observe all reviews as a reactive stream, ordered by most recently
     * updated first. Emits a new list whenever the underlying table changes.
     */
    fun getAllReviews(): Flow<List<Review>>

    /**
     * Observe all reviews written for a specific movie.
     * Emits a new list on every table change.
     */
    fun getReviewsForMovie(tmdbMovieId: Int): Flow<List<Review>>

    /**
     * Load a single review by its local database [id], or null if not found.
     */
    suspend fun getReviewById(id: Int): Review?

    /**
     * Insert or update a review. If a row with the same [Review.id] already
     * exists it is replaced; otherwise a new row is inserted.
     */
    suspend fun upsertReview(review: Review)

    /**
     * Permanently delete a review. Uses [Review.id] to identify the row.
     */
    suspend fun deleteReview(review: Review)
}
