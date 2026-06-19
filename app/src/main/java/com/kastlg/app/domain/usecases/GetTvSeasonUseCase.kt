package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.Season
import com.kastlg.app.domain.repositories.MovieRepository

class GetTvSeasonUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(tvShowId: Int, seasonNumber: Int): Season =
        repository.getTvSeason(tvShowId, seasonNumber)
}
