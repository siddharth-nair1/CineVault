package com.personal.cinevault.domain.usecase

import com.personal.cinevault.domain.model.Review
import com.personal.cinevault.domain.repository.ReviewRepository

/**
 * Use case that inserts or replaces a [Review] in the local database.
 *
 * Wraps [ReviewRepository.upsertReview] in a [Result] so callers get
 * uniform success / failure handling without needing to catch exceptions
 * directly.
 */
class SaveReviewUseCase(private val reviewRepository: ReviewRepository) {

    /**
     * Persist [review]. Returns [Result.success] on a clean write,
     * [Result.failure] if the DAO throws.
     */
    suspend operator fun invoke(review: Review): Result<Unit> =
        runCatching { reviewRepository.upsertReview(review) }
}
