package com.personal.cinevault.ui.screens.logmovie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.LogRepository
import com.personal.cinevault.domain.usecase.GetMovieDetailsUseCase
import com.personal.cinevault.domain.usecase.LogMovieUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LogMovieViewModel(
    private val movieId: Int,
    /** 0 = new entry; positive value = editing an existing entry with this Room id. */
    private val existingEntryId: Int = 0,
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    private val logMovieUseCase: LogMovieUseCase,
    private val logRepository: LogRepository
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

    // ── Mode indicator ────────────────────────────────────────────────────────

    /** True when editing an existing entry (pre-populated form), false for new entry. */
    val isEditMode: Boolean get() = existingEntryId > 0

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
        if (existingEntryId > 0) loadExistingEntry()
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

    /**
     * Load the existing log entry in parallel with the movie details and
     * pre-populate all form fields. Called only in edit mode.
     */
    private fun loadExistingEntry() {
        viewModelScope.launch {
            val entry = logRepository.getLogById(existingEntryId) ?: return@launch

            // Rating is stored at 0–10 scale; display is 0–5 (half-star).
            _rating.value = (entry.rating ?: 0f) / 2f

            _isLiked.value = entry.liked
            _isRewatch.value = entry.rewatch
            _reviewText.value = entry.review ?: ""

            // Parse ISO date string → epoch-millis for the DatePicker.
            _watchedDate.value = entry.watchedDate
                ?.runCatching {
                    LocalDate.parse(this)
                        .atStartOfDay(ZoneId.of("UTC"))
                        .toInstant()
                        .toEpochMilli()
                }
                ?.getOrNull()
                ?: todayAsEpochMillis()
        }
    }

    // ── Form events ───────────────────────────────────────────────────────────

    fun onRatingChange(stars: Float) { _rating.value = stars.coerceIn(0f, 5f) }
    fun onLikedToggle()  { _isLiked.value   = !_isLiked.value   }
    fun onRewatchToggle() { _isRewatch.value = !_isRewatch.value }
    fun onDateChange(epochMillis: Long) { _watchedDate.value = epochMillis }
    fun onReviewChange(text: String)    { _reviewText.value  = text        }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val movie = _movie.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            // Convert display-scale (0.5–5.0) → storage-scale (1.0–10.0).
            val storageRating = _rating.value.takeIf { it > 0f }?.let { it * 2f }

            // Convert epoch-millis → ISO-8601 date string ("YYYY-MM-DD").
            val watchedDateStr = Instant.ofEpochMilli(_watchedDate.value)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()

            val entry = LogEntry(
                // Preserve the existing id in edit mode so Room does UPDATE, not INSERT.
                id = existingEntryId,
                movie = movie,
                rating = storageRating,
                liked = _isLiked.value,
                rewatch = _isRewatch.value,
                review = _reviewText.value.trim().takeIf { it.isNotEmpty() },
                watchedDate = watchedDateStr
            )

            logMovieUseCase(entry)
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
