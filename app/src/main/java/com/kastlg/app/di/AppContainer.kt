package com.kastlg.app.di

import android.content.Context
import com.kastlg.app.BuildConfig
import com.kastlg.app.data.local.KastLgDatabase
import com.kastlg.app.data.remote.TmdbNetwork
import com.kastlg.app.data.remote.TmdbTokenStore
import com.kastlg.app.data.repository.RoomFavoriteRepository
import com.kastlg.app.data.repository.RoomHistoryRepository
import com.kastlg.app.data.repository.WebOsTvRepository
import com.kastlg.app.data.tv.SsapClient
import com.kastlg.app.domain.repositories.MovieRepository
import com.kastlg.app.domain.usecases.DiscoverMoviesUseCase
import com.kastlg.app.domain.usecases.GetMovieDetailUseCase
import com.kastlg.app.domain.usecases.GetMovieGenresUseCase
import com.kastlg.app.domain.usecases.GetNowPlayingMoviesUseCase
import com.kastlg.app.domain.usecases.GetPopularTvShowsUseCase
import com.kastlg.app.domain.usecases.GetTopRatedMoviesUseCase
import com.kastlg.app.domain.usecases.GetTrendingMoviesUseCase
import com.kastlg.app.domain.usecases.GetTvGenresUseCase
import com.kastlg.app.domain.usecases.GetTvSeasonUseCase
import com.kastlg.app.domain.usecases.GetTvShowDetailUseCase
import com.kastlg.app.domain.usecases.SearchMoviesUseCase
import com.kastlg.app.domain.usecases.SearchTvShowsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppContainer {
    private lateinit var applicationContext: Context

    val tmdbTokenStore by lazy {
        check(::applicationContext.isInitialized) {
            "AppContainer.initialize(context) must be called first."
        }
        TmdbTokenStore(applicationContext)
    }

    private val database by lazy {
        check(::applicationContext.isInitialized) {
            "AppContainer.initialize(context) must be called before local persistence is used."
        }
        KastLgDatabase.create(applicationContext)
    }

    val favoriteRepository by lazy {
        RoomFavoriteRepository(database.favoriteDao())
    }

    val historyRepository by lazy {
        RoomHistoryRepository(database.historyDao())
    }

    private val ssapClient by lazy { SsapClient() }

    val tvRepository by lazy {
        WebOsTvRepository(database.tvConfigDao(), ssapClient)
    }

    private val _hasToken = MutableStateFlow(false)
    val hasToken: StateFlow<Boolean> = _hasToken

    private var _movieRepository: MovieRepository? = null

    private var _getMovieGenres: GetMovieGenresUseCase? = null
    private var _discoverMovies: DiscoverMoviesUseCase? = null
    private var _searchMovies: SearchMoviesUseCase? = null
    private var _getMovieDetail: GetMovieDetailUseCase? = null
    private var _getTrendingMovies: GetTrendingMoviesUseCase? = null
    private var _getNowPlayingMovies: GetNowPlayingMoviesUseCase? = null
    private var _getTopRatedMovies: GetTopRatedMoviesUseCase? = null
    private var _getPopularTvShows: GetPopularTvShowsUseCase? = null
    private var _searchTvShows: SearchTvShowsUseCase? = null
    private var _getTvShowDetail: GetTvShowDetailUseCase? = null
    private var _getTvSeason: GetTvSeasonUseCase? = null
    private var _getTvGenres: GetTvGenresUseCase? = null

    val getMovieGenres: GetMovieGenresUseCase
        get() = _getMovieGenres ?: error("TMDB not initialized.")
    val discoverMovies: DiscoverMoviesUseCase
        get() = _discoverMovies ?: error("TMDB not initialized.")
    val searchMovies: SearchMoviesUseCase
        get() = _searchMovies ?: error("TMDB not initialized.")
    val getMovieDetail: GetMovieDetailUseCase
        get() = _getMovieDetail ?: error("TMDB not initialized.")
    val getTrendingMovies: GetTrendingMoviesUseCase
        get() = _getTrendingMovies ?: error("TMDB not initialized.")
    val getNowPlayingMovies: GetNowPlayingMoviesUseCase
        get() = _getNowPlayingMovies ?: error("TMDB not initialized.")
    val getTopRatedMovies: GetTopRatedMoviesUseCase
        get() = _getTopRatedMovies ?: error("TMDB not initialized.")
    val getPopularTvShows: GetPopularTvShowsUseCase
        get() = _getPopularTvShows ?: error("TMDB not initialized.")
    val searchTvShows: SearchTvShowsUseCase
        get() = _searchTvShows ?: error("TMDB not initialized.")
    val getTvShowDetail: GetTvShowDetailUseCase
        get() = _getTvShowDetail ?: error("TMDB not initialized.")

    val getTvSeason: GetTvSeasonUseCase
        get() = _getTvSeason ?: error("TMDB not initialized.")

    val getTvGenres: GetTvGenresUseCase
        get() = _getTvGenres ?: error("TMDB not initialized.")

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    suspend fun initializeTmdbRepository() {
        val storedToken = tmdbTokenStore.getToken()
        val token = when {
            storedToken.isNotEmpty() -> storedToken
            BuildConfig.TMDB_ACCESS_TOKEN.isNotEmpty() -> BuildConfig.TMDB_ACCESS_TOKEN
            else -> ""
        }
        applyToken(token)
    }

    fun refreshTmdbRepository(token: String) {
        applyToken(token)
    }

    private fun applyToken(token: String) {
        _movieRepository = TmdbNetwork.createRepository(token)
        _hasToken.value = token.isNotEmpty()
        val repo = _movieRepository!!
        _getMovieGenres = GetMovieGenresUseCase(repo)
        _discoverMovies = DiscoverMoviesUseCase(repo)
        _searchMovies = SearchMoviesUseCase(repo)
        _getMovieDetail = GetMovieDetailUseCase(repo)
        _getTrendingMovies = GetTrendingMoviesUseCase(repo)
        _getNowPlayingMovies = GetNowPlayingMoviesUseCase(repo)
        _getTopRatedMovies = GetTopRatedMoviesUseCase(repo)
        _getPopularTvShows = GetPopularTvShowsUseCase(repo)
        _searchTvShows = SearchTvShowsUseCase(repo)
        _getTvShowDetail = GetTvShowDetailUseCase(repo)
        _getTvSeason = GetTvSeasonUseCase(repo)
        _getTvGenres = GetTvGenresUseCase(repo)
    }
}
