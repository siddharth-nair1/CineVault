package com.personal.cinevault.ui.screens.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.Review
import com.personal.cinevault.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewDetailViewModel(
    private val reviewId: Long,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _review = MutableStateFlow<Review?>(null)
    val review: StateFlow<Review?> = _review.asStateFlow()

    /** True while a delete operation is in progress. */
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    /**
     * Set to true once delete completes so the UI can pop the back-stack.
     */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        loadReview()
    }

    private fun loadReview() {
        viewModelScope.launch {
            _review.value = reviewRepository.getReviewById(reviewId.toInt())
        }
    }

    /**
     * Toggle [Review.containsSpoilers]. Persists the change immediately so it
     * survives navigation.
     */
    fun toggleSpoiler() {
        val current = _review.value ?: return
        val updated = current.copy(containsSpoilers = !current.containsSpoilers)
        _review.value = updated
        viewModelScope.launch {
            reviewRepository.upsertReview(updated)
        }
    }

    /**
     * Permanently delete the review and signal the UI to pop back.
     */
    fun deleteReview() {
        val current = _review.value ?: return
        viewModelScope.launch {
            _isDeleting.value = true
            reviewRepository.deleteReview(current)
            _isDeleting.value = false
            _deleted.value = true
        }
    }
}
