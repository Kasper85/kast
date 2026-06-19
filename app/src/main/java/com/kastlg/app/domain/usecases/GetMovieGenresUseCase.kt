package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.repositories.MovieRepository

class GetMovieGenresUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(): List<Genre> = repository.getMovieGenres()
}
