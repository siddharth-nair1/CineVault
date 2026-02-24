package com.personal.cinevault.domain.model

/**
 * Clean domain model for a movie — framework-free, used across all layers.
 */
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseYear: Int?,
    val runtime: Int?,
    val rating: Float?,
    val genres: List<String>
)
