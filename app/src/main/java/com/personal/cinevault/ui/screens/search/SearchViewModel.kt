package com.personal.cinevault.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.ListMovie
import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.CineListRepository
import com.personal.cinevault.domain.usecase.SearchMoviesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val cineListRepository: CineListRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Non-null when this search is in "add to list" mode. */
    private val _addToListId = MutableStateFlow<Int?>(null)
    val addToListId: StateFlow<Int?> = _addToListId.asStateFlow()

    /** Emits once when a movie has just been added — consumed by the UI to show a snackbar. */
    private val _addedMovie = MutableStateFlow<String?>(null)
    val addedMovie: StateFlow<String?> = _addedMovie.asStateFlow()

    private var searchJob: Job? = null

    /** Call from NavGraph after recomposing with the listId argument. */
    fun setAddToListMode(listId: Int?) {
        _addToListId.value = listId
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query

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
                .onSuccess { movies -> _searchResults.value = movies }
                .onFailure { e -> _error.value = e.message ?: "Search failed" }

            _isLoading.value = false
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _error.value = null
    }

    /**
     * Add [movie] to the list identified by [listId].
     * Emits the movie title to [addedMovie] so the UI can show a confirmation.
     */
    fun addMovieToList(listId: Int, movie: Movie) {
        viewModelScope.launch {
            cineListRepository.addMovieToList(
                ListMovie(
                    listId      = listId,
                    tmdbMovieId = movie.id,
                    movieTitle  = movie.title,
                    posterPath  = movie.posterPath,
                    voteAverage = movie.rating,
                    releaseYear = movie.releaseYear?.toString()
                )
            )
            _addedMovie.value = movie.title
        }
    }

    /** Call after the UI has consumed the added-movie event. */
    fun onAddedMovieConsumed() {
        _addedMovie.value = null
    }
}
