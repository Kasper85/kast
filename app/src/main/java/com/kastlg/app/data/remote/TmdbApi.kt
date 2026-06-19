package com.kastlg.app.data.remote

import com.kastlg.app.data.remote.dto.GenreListDto
import com.kastlg.app.data.remote.dto.MovieDetailDto
import com.kastlg.app.data.remote.dto.MoviePageDto
import com.kastlg.app.data.remote.dto.SeasonDto
import com.kastlg.app.data.remote.dto.TvShowDetailDto
import com.kastlg.app.data.remote.dto.TvShowPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    // ── Movies ──

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
    ): GenreListDto

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genreId: Int? = null,
        @Query("page") page: Int = 1,
    ): MoviePageDto

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1,
    ): MoviePageDto

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") movieId: Int,
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
    ): MovieDetailDto

    // ── Movie categories ──

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
    ): MoviePageDto

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
        @Query("page") page: Int = 1,
    ): MoviePageDto

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
        @Query("page") page: Int = 1,
    ): MoviePageDto

    // ── TV Shows ──

    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
        @Query("page") page: Int = 1,
    ): TvShowPageDto

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("query") query: String,
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
        @Query("page") page: Int = 1,
    ): TvShowPageDto

    @GET("tv/{id}")
    suspend fun getTvShowDetails(
        @Path("id") tvShowId: Int,
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
    ): TvShowDetailDto

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
    ): GenreListDto

    // ── Seasons ──

    @GET("tv/{id}/season/{season_number}")
    suspend fun getTvSeason(
        @Path("id") tvShowId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = TmdbNetwork.LANGUAGE,
    ): SeasonDto
}
