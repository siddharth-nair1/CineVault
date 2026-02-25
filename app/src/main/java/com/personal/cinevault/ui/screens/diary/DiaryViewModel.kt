package com.personal.cinevault.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents a single item in the diary [LazyColumn] — either a sticky
 * month/year section header or a log-entry row.
 */
sealed class DiaryListItem {
    /** A month/year separator, e.g. "February 2026". */
    data class Header(val label: String) : DiaryListItem()
    /** A single log-entry row. */
    data class Entry(val log: LogEntry) : DiaryListItem()
}

class DiaryViewModel(
    private val logRepository: LogRepository   // keep reference for delete
) : ViewModel() {

    // ── Grouped log list ──────────────────────────────────────────────────────

    /**
     * All log entries grouped by month, ordered newest-first within each
     * group, with a [DiaryListItem.Header] injected before each new month.
     */
    val logs: StateFlow<List<DiaryListItem>> = logRepository
        .getAllLogs()
        .map { entries -> entries.toGroupedDiaryItems() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Delete ────────────────────────────────────────────────────────────────

    /** The entry awaiting confirmation before deletion, or null if no dialog shown. */
    private val _pendingDelete = MutableStateFlow<LogEntry?>(null)
    val pendingDelete: StateFlow<LogEntry?> = _pendingDelete.asStateFlow()

    /** Called when the user completes a swipe — shows the confirmation dialog. */
    fun requestDelete(entry: LogEntry) {
        _pendingDelete.value = entry
    }

    /** Called when the user taps "Cancel" in the delete confirmation dialog. */
    fun cancelDelete() {
        _pendingDelete.value = null
    }

    /** Called when the user taps "Delete" in the confirmation dialog. */
    fun confirmDelete() {
        val entry = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch { logRepository.deleteLog(entry) }
    }
}

// ── Grouping helper ───────────────────────────────────────────────────────────

private fun List<LogEntry>.toGroupedDiaryItems(): List<DiaryListItem> {
    if (isEmpty()) return emptyList()

    val grouped = groupBy { entry ->
        entry.watchedDate
            ?.takeIf { it.length >= 7 }
            ?.let { iso ->
                val year  = iso.substring(0, 4).toIntOrNull() ?: return@let null
                val month = iso.substring(5, 7).toIntOrNull() ?: return@let null
                java.time.YearMonth.of(year, month)
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            }
            ?: "Unknown date"
    }

    return buildList {
        grouped.forEach { (monthLabel, entries) ->
            add(DiaryListItem.Header(monthLabel))
            entries.forEach { add(DiaryListItem.Entry(it)) }
        }
    }
}
