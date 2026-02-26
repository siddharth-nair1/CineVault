package com.personal.cinevault.data.repository

import com.personal.cinevault.data.local.dao.ReviewDao
import com.personal.cinevault.data.local.toDomain
import com.personal.cinevault.data.local.toEntity
import com.personal.cinevault.domain.model.Review
import com.personal.cinevault.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [ReviewRepository] backed by Room via [ReviewDao].
 *
 * All entity ↔ domain mapping is delegated to the extension functions in
 * `ReviewMappers.kt`.
 */
class ReviewRepositoryImpl(
    private val dao: ReviewDao
) : ReviewRepository {

    override fun getAllReviews(): Flow<List<Review>> =
        dao.getAllReviews().map { entities -> entities.map { it.toDomain() } }

    override fun getReviewsForMovie(tmdbMovieId: Int): Flow<List<Review>> =
        dao.getReviewsForMovie(tmdbMovieId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getReviewById(id: Int): Review? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertReview(review: Review) = dao.upsert(review.toEntity())

    override suspend fun deleteReview(review: Review) = dao.delete(review.toEntity())
}
