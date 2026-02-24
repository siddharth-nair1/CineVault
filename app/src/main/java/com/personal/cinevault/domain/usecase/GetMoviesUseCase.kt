package com.personal.cinevault.domain.usecase

import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.MovieRepository

class GetMovieDetailsUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movieId: Int): Result<Movie> =
        repository.getMovieDetails(movieId)
}
