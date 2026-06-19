package com.kastlg.app.domain.usecases

import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.repositories.MovieRepository

class SearchTvShowsUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(query: String): List<TvShow> = repository.searchTvShows(query)
}
