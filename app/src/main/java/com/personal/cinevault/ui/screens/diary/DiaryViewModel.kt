package com.personal.cinevault.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
    logRepository: LogRepository
) : ViewModel() {

    /**
     * All log entries grouped by month, ordered newest-first within each
     * group, with a [DiaryListItem.Header] injected before each new month.
     *
     * Built reactively from [LogRepository.getAllLogs] so the list updates
     * automatically whenever the user logs a new film.
     */
    val logs: StateFlow<List<DiaryListItem>> = logRepository
        .getAllLogs()
        .map { entries -> entries.toGroupedDiaryItems() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

// ── Grouping helper ───────────────────────────────────────────────────────────

/**
 * Transform a flat list of [LogEntry] into a list of [DiaryListItem] with
 * month/year headers interspersed. Entries without a [LogEntry.watchedDate]
 * are grouped under an "Unknown date" header and placed at the end.
 *
 * Input is assumed to already be ordered by [LogEntry.watchedDate] DESC
 * (as returned by [LogEntryDao.getAllLogs]).
 */
private fun List<LogEntry>.toGroupedDiaryItems(): List<DiaryListItem> {
    if (isEmpty()) return emptyList()

    // Group by "MMMM yyyy" label derived from the ISO-8601 watchedDate string.
    val grouped = groupBy { entry ->
        entry.watchedDate
            ?.takeIf { it.length >= 7 }               // need at least "YYYY-MM"
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
