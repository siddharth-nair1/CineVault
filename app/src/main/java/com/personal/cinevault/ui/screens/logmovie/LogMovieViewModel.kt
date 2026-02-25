package com.personal.cinevault.ui.screens.logmovie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.usecase.GetMovieDetailsUseCase
import com.personal.cinevault.domain.usecase.LogMovieUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class LogMovieViewModel(
    private val movieId: Int,
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    private val logMovieUseCase: LogMovieUseCase
) : ViewModel() {

    // ── Movie detail ──────────────────────────────────────────────────────────

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _isLoadingMovie = MutableStateFlow(false)
    val isLoadingMovie: StateFlow<Boolean> = _isLoadingMovie.asStateFlow()

    // ── Form state ────────────────────────────────────────────────────────────

    /** Half-star rating on a 0.0–5.0 display scale. 0.0 = unrated. */
    private val _rating = MutableStateFlow(0f)
    val rating: StateFlow<Float> = _rating.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _isRewatch = MutableStateFlow(false)
    val isRewatch: StateFlow<Boolean> = _isRewatch.asStateFlow()

    /** Selected watch date as epoch-millis (for DatePicker interop). Defaults to today. */
    private val _watchedDate = MutableStateFlow(todayAsEpochMillis())
    val watchedDate: StateFlow<Long> = _watchedDate.asStateFlow()

    private val _reviewText = MutableStateFlow("")
    val reviewText: StateFlow<String> = _reviewText.asStateFlow()

    // ── Save state ────────────────────────────────────────────────────────────

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Set to true once save completes — the Screen observes this to pop backstack. */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadMovie()
    }

    private fun loadMovie() {
        viewModelScope.launch {
            _isLoadingMovie.value = true
            getMovieDetailsUseCase(movieId)
                .onSuccess { _movie.value = it }
                .onFailure { _error.value = it.message ?: "Failed to load movie" }
            _isLoadingMovie.value = false
        }
    }

    // ── Form events ───────────────────────────────────────────────────────────

    /** Set half-star rating (0.5 … 5.0). Pass 0f to clear the rating. */
    fun onRatingChange(stars: Float) {
        _rating.value = stars.coerceIn(0f, 5f)
    }

    fun onLikedToggle() { _isLiked.value = !_isLiked.value }

    fun onRewatchToggle() { _isRewatch.value = !_isRewatch.value }

    /** Update watched date from the DatePicker. [epochMillis] is UTC midnight. */
    fun onDateChange(epochMillis: Long) { _watchedDate.value = epochMillis }

    fun onReviewChange(text: String) { _reviewText.value = text }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val movie = _movie.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            logMovieUseCase(
                movie = movie,
                displayRating = _rating.value.takeIf { it > 0f },
                liked = _isLiked.value,
                isRewatch = _isRewatch.value,
                watchedDateMs = _watchedDate.value,
                review = _reviewText.value
            )
                .onSuccess { _saved.value = true }
                .onFailure { _error.value = it.message ?: "Failed to save" }
            _isSaving.value = false
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun todayAsEpochMillis(): Long =
        LocalDate.now()
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
}
