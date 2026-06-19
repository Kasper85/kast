package com.kastlg.app.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.domain.repositories.FavoriteRepository
import com.kastlg.app.domain.repositories.HistoryRepository
import com.kastlg.app.domain.repositories.TvRepository
import com.kastlg.app.domain.usecases.GetMovieDetailUseCase

class MovieDetailViewModelFactory(
    private val movieId: Int,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val tvRepository: TvRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MovieDetailViewModel::class.java))
        return MovieDetailViewModel(
            movieId = movieId,
            getMovieDetail = getMovieDetail,
            favoriteRepository = favoriteRepository,
            historyRepository = historyRepository,
            tvRepository = tvRepository,
        ) as T
    }
}
