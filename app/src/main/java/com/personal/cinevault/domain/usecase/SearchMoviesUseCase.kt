package com.personal.cinevault.domain.usecase

import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.MovieRepository

class SearchMoviesUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(query: String): Result<List<Movie>> =
        repository.searchMovies(query)
}
