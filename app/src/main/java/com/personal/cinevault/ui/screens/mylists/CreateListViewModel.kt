package com.personal.cinevault.ui.screens.mylists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.CineList
import com.personal.cinevault.domain.repository.CineListRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Create List form screen.
 *
 * Owns the save logic; the composable drives UI state directly via
 * [remember] variables to keep the screen stateless.
 */
class CreateListViewModel(
    private val repository: CineListRepository
) : ViewModel() {

    /**
     * Persist a new [CineList] and invoke [onCreated] with the assigned ID
     * once the insert completes (called on the main dispatcher by
     * [viewModelScope]).
     */
    fun createList(
        name: String,
        description: String?,
        icon: String?,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newId = repository.createList(
                CineList(
                    name        = name.trim(),
                    description = description?.trim()?.ifBlank { null },
                    icon        = icon?.trim()?.ifBlank { null }
                )
            )
            onCreated(newId)
        }
    }
}
