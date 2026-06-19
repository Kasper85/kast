package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.repositories.MovieRepository

class GetTrendingMoviesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(): List<Movie> = repository.getTrendingMovies()
}
