package com.personal.cinevault.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.WatchlistItem
import com.personal.cinevault.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Watchlist screen.
 *
 * Exposes all watchlist entries as a [StateFlow] backed by [WatchlistRepository].
 * Provides [removeFromWatchlist] to let the user delete an entry via long-press
 * context menu.
 */
class WatchlistViewModel(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    /**
     * All watchlist entries ordered by most-recently-added, mapped to the
     * domain [WatchlistItem] model. Emits a new list whenever the Room table
     * changes, keeping the UI reactive without manual refreshes.
     */
    val watchlistItems: StateFlow<List<WatchlistItem>> =
        watchlistRepository.getAllWatchlist()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /**
     * Remove the movie identified by [tmdbMovieId] from the watchlist.
     * Delegates to the repository which does nothing if the entry is absent.
     */
    fun removeFromWatchlist(tmdbMovieId: Int) {
        viewModelScope.launch {
            watchlistRepository.removeFromWatchlist(tmdbMovieId)
        }
    }
}
