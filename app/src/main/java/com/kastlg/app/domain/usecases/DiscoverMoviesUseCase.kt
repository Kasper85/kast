package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.repositories.MovieRepository

class DiscoverMoviesUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(genreId: Int? = null): List<Movie> =
        repository.discoverMovies(genreId)
}
