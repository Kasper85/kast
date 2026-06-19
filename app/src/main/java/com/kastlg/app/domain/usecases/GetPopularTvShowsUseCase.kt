package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.repositories.MovieRepository

class GetPopularTvShowsUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(): List<TvShow> = repository.getPopularTvShows()
}
