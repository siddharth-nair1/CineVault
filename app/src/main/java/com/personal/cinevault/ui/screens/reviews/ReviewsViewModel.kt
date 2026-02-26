package com.personal.cinevault.ui.screens.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.Review
import com.personal.cinevault.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReviewsViewModel(
    reviewRepository: ReviewRepository
) : ViewModel() {

    /** All user-written reviews, ordered by most recently updated first. */
    val reviews: StateFlow<List<Review>> =
        reviewRepository.getAllReviews()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
