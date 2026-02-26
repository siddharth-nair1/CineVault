package com.personal.cinevault.ui.screens.mylists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.CineList
import com.personal.cinevault.domain.model.ListMovie
import com.personal.cinevault.domain.repository.CineListRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the List Detail screen.
 *
 * Instantiated by Koin with parametersOf(listId).
 */
class ListDetailViewModel(
    val listId: Int,
    private val repository: CineListRepository
) : ViewModel() {

    /**
     * The [CineList] metadata (name, description, icon…) for this list.
     * Stays null until Room emits the first snapshot.
     */
    val cineList: StateFlow<CineList?> =
        repository.getAllLists()
            .map { lists -> lists.firstOrNull { it.id == listId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    /** All movies in this list, ordered by sortOrder then addedAt. */
    val movies: StateFlow<List<ListMovie>> =
        repository.getMoviesInList(listId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Remove [tmdbMovieId] from this list. */
    fun removeMovie(tmdbMovieId: Int) {
        viewModelScope.launch { repository.removeMovieFromList(listId, tmdbMovieId) }
    }

    /** Persist mutations to the list's name, description, icon, etc. */
    fun updateList(updated: CineList) {
        viewModelScope.launch { repository.updateList(updated) }
    }
}
