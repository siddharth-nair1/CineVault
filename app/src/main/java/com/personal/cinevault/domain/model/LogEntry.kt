package com.personal.cinevault.domain.model

import java.time.LocalDate

/**
 * Represents a user's diary entry for a watched film.
 * Stub — will be backed by Room when PersonalDatabase is implemented.
 */
data class LogEntry(
    val id: Int,
    val movie: Movie,
    val watchedDate: LocalDate,
    val rating: Float?,
    val review: String?
)
