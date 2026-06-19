package com.kastlg.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.domain.usecases.GetMovieGenresUseCase
import com.kastlg.app.domain.usecases.GetNowPlayingMoviesUseCase
import com.kastlg.app.domain.usecases.GetPopularTvShowsUseCase
import com.kastlg.app.domain.usecases.GetTopRatedMoviesUseCase
import com.kastlg.app.domain.usecases.GetTrendingMoviesUseCase
import com.kastlg.app.domain.usecases.GetTvGenresUseCase
import com.kastlg.app.domain.usecases.SearchMoviesUseCase
import com.kastlg.app.domain.usecases.SearchTvShowsUseCase

class HomeViewModelFactory(
    private val getTrendingMovies: GetTrendingMoviesUseCase,
    private val getNowPlayingMovies: GetNowPlayingMoviesUseCase,
    private val getTopRatedMovies: GetTopRatedMoviesUseCase,
    private val getPopularTvShows: GetPopularTvShowsUseCase,
    private val searchMovies: SearchMoviesUseCase,
    private val searchTvShows: SearchTvShowsUseCase,
    private val getMovieGenres: GetMovieGenresUseCase,
    private val getTvGenres: GetTvGenresUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(
            getTrendingMovies = getTrendingMovies,
            getNowPlayingMovies = getNowPlayingMovies,
            getTopRatedMovies = getTopRatedMovies,
            getPopularTvShows = getPopularTvShows,
            searchMovies = searchMovies,
            searchTvShows = searchTvShows,
            getMovieGenres = getMovieGenres,
            getTvGenres = getTvGenres,
        ) as T
    }
}
