package com.kastlg.app.data.repository

import com.kastlg.app.data.remote.TmdbApi
import com.kastlg.app.data.remote.dto.toDomain
import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.models.Season
import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.models.TvShowDetail
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import com.kastlg.app.domain.repositories.MovieRepository

class TmdbMovieRepository(
    private val api: TmdbApi,
    private val hasAccessToken: Boolean,
) : MovieRepository {
    override suspend fun getMovieGenres(): List<Genre> {
        requireAccessToken()
        return api.getMovieGenres().genres.map { it.toDomain() }
    }

    override suspend fun getTvGenres(): List<Genre> {
        requireAccessToken()
        return api.getTvGenres().genres.map { it.toDomain() }
    }

    override suspend fun discoverMovies(genreId: Int?): List<Movie> {
        requireAccessToken()
        return api.discoverMovies(genreId = genreId).results.map { it.toDomain() }
    }

    override suspend fun searchMovies(query: String): List<Movie> {
        requireAccessToken()
        return api.searchMovies(query.trim()).results.map { it.toDomain() }
    }

    override suspend fun getMovieDetail(movieId: Int): MovieDetail {
        requireAccessToken()
        return api.getMovieDetails(movieId).toDomain()
    }

    override suspend fun getTrendingMovies(): List<Movie> {
        requireAccessToken()
        return api.getTrendingMovies().results.map { it.toDomain() }
    }

    override suspend fun getNowPlayingMovies(): List<Movie> {
        requireAccessToken()
        return api.getNowPlayingMovies().results.map { it.toDomain() }
    }

    override suspend fun getTopRatedMovies(): List<Movie> {
        requireAccessToken()
        return api.getTopRatedMovies().results.map { it.toDomain() }
    }

    override suspend fun getPopularTvShows(): List<TvShow> {
        requireAccessToken()
        return api.getPopularTvShows().results.map { it.toDomain() }
    }

    override suspend fun searchTvShows(query: String): List<TvShow> {
        requireAccessToken()
        return api.searchTvShows(query.trim()).results.map { it.toDomain() }
    }

    override suspend fun getTvShowDetail(tvShowId: Int): TvShowDetail {
        requireAccessToken()
        return api.getTvShowDetails(tvShowId).toDomain()
    }

    override suspend fun getTvSeason(tvShowId: Int, seasonNumber: Int): Season {
        requireAccessToken()
        return api.getTvSeason(tvShowId, seasonNumber).toDomain()
    }

    private fun requireAccessToken() {
        if (!hasAccessToken) {
            throw MissingTmdbTokenException()
        }
    }
}
