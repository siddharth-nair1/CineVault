package com.personal.cinevault.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.usecase.SearchMoviesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchMoviesUseCase: SearchMoviesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _searchQuery.value = query

        // Cancel any in-flight search and debounce 500ms
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(500L)
            _isLoading.value = true
            _error.value = null

            searchMoviesUseCase(query)
                .onSuccess { movies ->
                    _searchResults.value = movies
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Search failed"
                }

            _isLoading.value = false
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _error.value = null
    }
}
