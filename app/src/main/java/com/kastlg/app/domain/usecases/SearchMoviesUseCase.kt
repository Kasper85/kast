package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.repositories.MovieRepository

class SearchMoviesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(query: String): List<Movie> = repository.searchMovies(query)
}
