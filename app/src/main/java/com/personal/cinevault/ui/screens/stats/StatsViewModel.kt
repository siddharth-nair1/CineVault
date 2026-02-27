package com.personal.cinevault.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.UserStats
import com.personal.cinevault.domain.usecase.GetUserStatsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    getUserStatsUseCase: GetUserStatsUseCase,
) : ViewModel() {

    /**
     * Live [UserStats] derived from the entire diary.
     * Null only during the brief initialisation before the first DB emit.
     */
    val stats: StateFlow<UserStats?> = getUserStatsUseCase()
        .map { it }        // passthrough — gives us a clean Flow<UserStats>
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
