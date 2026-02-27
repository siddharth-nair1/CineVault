package com.personal.cinevault.ui.screens.moviedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.LogRepository
import com.personal.cinevault.domain.repository.WatchlistRepository
import com.personal.cinevault.domain.usecase.GetMovieDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovieDetailViewModel(
    private val movieId: Int,
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    private val watchlistRepository: WatchlistRepository,
    private val logRepository: LogRepository,
) : ViewModel() {

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Reactive watchlist state for this movie, driven directly from Room.
     * Emits `true` whenever a row for [movieId] exists in the watchlist table.
     */
    val isInWatchlist: StateFlow<Boolean> =
        watchlistRepository.isInWatchlist(movieId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    /**
     * Reactive log entry for this movie, or null if the movie has not been logged yet.
     * The UI uses this to show "Write Review" vs "Edit Review" and to pass the
     * existing entry id to the log screen.
     */
    val logEntry: StateFlow<LogEntry?> =
        logRepository.getLogForMovie(movieId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    init {
        loadMovie()
    }

    private fun loadMovie() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getMovieDetailsUseCase(movieId)
                .onSuccess { _movie.value = it }
                .onFailure { _error.value = it.message ?: "Failed to load movie" }
            _isLoading.value = false
        }
    }

    /**
     * Toggle the watchlist state for the currently loaded movie.
     * Uses the live [isInWatchlist] value to decide whether to add or remove.
     */
    fun toggleWatchlist() {
        val movie = _movie.value ?: return
        viewModelScope.launch {
            if (isInWatchlist.value) {
                watchlistRepository.removeFromWatchlist(movieId)
            } else {
                watchlistRepository.addToWatchlist(movie)
            }
        }
    }

    fun retry() = loadMovie()
}
