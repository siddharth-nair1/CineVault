package com.personal.cinevault.domain.usecase

import com.personal.cinevault.domain.model.Movie
import com.personal.cinevault.domain.repository.MovieRepository

class GetTrendingUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(): Result<List<Movie>> =
        repository.getTrending()
}
