package com.kastlg.app.presentation.tvdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.domain.repositories.FavoriteRepository
import com.kastlg.app.domain.usecases.GetTvSeasonUseCase
import com.kastlg.app.domain.usecases.GetTvShowDetailUseCase
import com.kastlg.app.domain.usecases.ResolveFlixcornSeriesSlugUseCase

class TvShowDetailViewModelFactory(
    private val tvShowId: Int,
    private val getTvShowDetail: GetTvShowDetailUseCase,
    private val getTvSeason: GetTvSeasonUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val resolveFlixcornSeriesSlug: ResolveFlixcornSeriesSlugUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TvShowDetailViewModel::class.java))
        return TvShowDetailViewModel(
            tvShowId = tvShowId,
            getTvShowDetail = getTvShowDetail,
            getTvSeason = getTvSeason,
            favoriteRepository = favoriteRepository,
            resolveFlixcornSeriesSlug = resolveFlixcornSeriesSlug,
        ) as T
    }
}
