package com.personal.cinevault.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── Format helper ─────────────────────────────────────────────────────────────

/** Date format used everywhere a watch-date is shown as a human-readable string. */
private val DIARY_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d, yyyy")   // e.g. "March 15, 2025"

/**
 * Format [epochMillis] (UTC midnight from the Material3 DatePicker) as a
 * human-readable date string using the [DIARY_DATE_FORMATTER] pattern.
 *
 * @return e.g. "March 15, 2025", or an empty string if [epochMillis] is null.
 */
fun formatWatchDate(epochMillis: Long?): String {
    epochMillis ?: return ""
    return runCatching {
        java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DIARY_DATE_FORMATTER)
    }.getOrDefault("")
}

// ── DatePicker dialog composable ──────────────────────────────────────────────

/**
 * A Material3 calendar dialog.
 *
 * Wrap your trigger button however you like; call [ShowDatePickerDialog] with
 * the current epoch-millis value and it will:
 *  1. Show a full-screen `DatePickerDialog` pre-selected to [initialDateMs]
 *     (defaults to today if null).
 *  2. Call [onDateSelected] with the chosen date as UTC-midnight epoch-millis
 *     when the user taps **OK**.
 *  3. Call [onDismiss] (or fall back to [onDismiss]) when the user taps
 *     **Cancel** or dismisses the dialog.
 *
 * @param initialDateMs Pre-selected date as epoch-millis. Defaults to today.
 * @param onDateSelected Called with the new epoch-millis when the user confirms.
 * @param onDismiss      Called when the dialog is dismissed without selecting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePickerDialog(
    initialDateMs: Long? = null,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val todayMs = LocalDate.now()
        .atStartOfDay(ZoneId.of("UTC"))
        .toInstant()
        .toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMs ?: todayMs
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
