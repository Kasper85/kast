package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.repositories.MovieRepository

class GetMovieDetailUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Int): MovieDetail = repository.getMovieDetail(movieId)
}
