package com.personal.cinevault.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.usecase.GetTrendingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getTrendingUseCase: GetTrendingUseCase
) : ViewModel() {

    private val _trendingMovies = MutableStateFlow<List<Movie>>(emptyList())
    val trendingMovies: StateFlow<List<Movie>> = _trendingMovies.asStateFlow()

    // Stub — will be populated from Room/PersonalDatabase when implemented
    private val _recentlyLogged = MutableStateFlow<List<LogEntry>>(emptyList())
    val recentlyLogged: StateFlow<List<LogEntry>> = _recentlyLogged.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
