package com.kastlg.app.presentation.tvdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.domain.repositories.TvRepository
import com.kastlg.app.domain.usecases.GetTvSeasonUseCase
import com.kastlg.app.domain.usecases.GetTvShowDetailUseCase

class TvShowDetailViewModelFactory(
    private val tvShowId: Int,
    private val getTvShowDetail: GetTvShowDetailUseCase,
    private val getTvSeason: GetTvSeasonUseCase,
    private val tvRepository: TvRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TvShowDetailViewModel::class.java))
        return TvShowDetailViewModel(
            tvShowId = tvShowId,
            getTvShowDetail = getTvShowDetail,
            getTvSeason = getTvSeason,
            tvRepository = tvRepository,
        ) as T
    }
}
