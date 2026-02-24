package com.personal.cinevault.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Movie Summary (used in lists & search) ───────────────────────────────────

data class MovieDto(
    @SerializedName("id")           val id: Int,
    @SerializedName("title")        val title: String,
    @SerializedName("overview")     val overview: String,
    @SerializedName("poster_path")  val posterPath: String?,
    @SerializedName("backdrop_path")val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("genre_ids")    val genreIds: List<Int>
)

// ─── Genre ────────────────────────────────────────────────────────────────────

data class GenreDto(
    @SerializedName("id")   val id: Int,
    @SerializedName("name") val name: String
)

// ─── Search Response ──────────────────────────────────────────────────────────

data class SearchResponse(
    @SerializedName("results")      val results: List<MovieDto>,
    @SerializedName("total_pages")  val totalPages: Int,
    @SerializedName("total_results")val totalResults: Int,
    @SerializedName("page")         val page: Int
)

// ─── Movie List Response (Trending / Popular) ─────────────────────────────────

data class MovieListResponse(
    @SerializedName("results")      val results: List<MovieDto>,
    @SerializedName("total_pages")  val totalPages: Int,
    @SerializedName("total_results")val totalResults: Int,
    @SerializedName("page")         val page: Int
)

// ─── Movie Detail Response ────────────────────────────────────────────────────

data class MovieDetailResponse(
    @SerializedName("id")            val id: Int,
    @SerializedName("title")         val title: String,
    @SerializedName("overview")      val overview: String,
    @SerializedName("poster_path")   val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date")  val releaseDate: String,
    @SerializedName("runtime")       val runtime: Int?,
    @SerializedName("vote_average")  val voteAverage: Double,
    @SerializedName("genres")        val genres: List<GenreDto>
)
