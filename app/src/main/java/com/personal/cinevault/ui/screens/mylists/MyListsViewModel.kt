package com.personal.cinevault.ui.screens.mylists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.CineList
import com.personal.cinevault.domain.repository.CineListRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the My Lists screen.
 *
 * Exposes all user-created lists as a reactive [StateFlow] and provides
 * [deleteList] for swipe-to-delete (or context menu) interactions.
 */
class MyListsViewModel(
    private val repository: CineListRepository
) : ViewModel() {

    /**
     * All user-created lists ordered by pinned status → sort order → last
     * updated. The UI re-renders automatically when the Room table changes.
     */
    val lists: StateFlow<List<CineList>> =
        repository.getAllLists()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Delete [list] and cascade-remove all its movie entries. */
    fun deleteList(list: CineList) {
        viewModelScope.launch { repository.deleteList(list) }
    }
}
