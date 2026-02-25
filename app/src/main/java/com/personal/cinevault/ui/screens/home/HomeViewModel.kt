package com.personal.cinevault.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.LogRepository
import com.personal.cinevault.domain.usecase.GetTrendingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getTrendingUseCase: GetTrendingUseCase,
    private val logRepository: LogRepository
) : ViewModel() {

    // ── Trending ──────────────────────────────────────────────────────────────

    private val _trendingMovies = MutableStateFlow<List<Movie>>(emptyList())
    val trendingMovies: StateFlow<List<Movie>> = _trendingMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Recently logged ───────────────────────────────────────────────────────

    /**
     * The 5 most recently watched entries from the user's Room diary, exposed
     * as a [StateFlow] that updates whenever the `log_entries` table changes.
     *
     * Uses [SharingStarted.WhileSubscribed] so the upstream Flow is cancelled
     * when no UI collector is active, restarting automatically on resubscription
     * (e.g. after process death or config change).
     */
    val recentlyLogged: StateFlow<List<LogEntry>> = logRepository
        .getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadTrending()
    }

    private fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            getTrendingUseCase()
                .onSuccess { _trendingMovies.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun retry() = loadTrending()
}
