package com.kastlg.app.domain.repositories

import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.models.Season
import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.models.TvShowDetail

interface MovieRepository {
    suspend fun getMovieGenres(): List<Genre>

    suspend fun getTvGenres(): List<Genre>

    suspend fun discoverMovies(genreId: Int? = null): List<Movie>

    suspend fun searchMovies(query: String): List<Movie>

    suspend fun getMovieDetail(movieId: Int): MovieDetail

    // ── Movie categories ──

    suspend fun getTrendingMovies(): List<Movie>

    suspend fun getNowPlayingMovies(): List<Movie>

    suspend fun getTopRatedMovies(): List<Movie>

    // ── TV Shows ──

    suspend fun getPopularTvShows(): List<TvShow>

    suspend fun searchTvShows(query: String): List<TvShow>

    suspend fun getTvShowDetail(tvShowId: Int): TvShowDetail

    suspend fun getTvSeason(tvShowId: Int, seasonNumber: Int): Season
}
