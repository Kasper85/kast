package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.TvShowDetail
import com.kastlg.app.domain.repositories.MovieRepository

class GetTvShowDetailUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(tvShowId: Int): TvShowDetail = repository.getTvShowDetail(tvShowId)
}
