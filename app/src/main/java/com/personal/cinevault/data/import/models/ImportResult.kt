package com.personal.cinevault.data.import.models

/**
 * Result of the Letterboxd Import process.
 */
data class ImportResult(
    val logsImported: Int = 0,
    val reviewsImported: Int = 0,
    val watchlistImported: Int = 0,
    val listsImported: Int = 0,
    val failedLookups: Int = 0
)
