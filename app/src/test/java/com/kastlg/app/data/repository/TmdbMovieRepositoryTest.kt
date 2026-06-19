package com.kastlg.app.data.repository

import com.kastlg.app.data.remote.TmdbApi
import com.kastlg.app.data.remote.dto.GenreDto
import com.kastlg.app.data.remote.dto.GenreListDto
import com.kastlg.app.data.remote.dto.MovieDetailDto
import com.kastlg.app.data.remote.dto.MovieDto
import com.kastlg.app.data.remote.dto.MoviePageDto
import com.kastlg.app.data.remote.dto.SeasonDto
import com.kastlg.app.data.remote.dto.TvShowDetailDto
import com.kastlg.app.data.remote.dto.TvShowPageDto
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class TmdbMovieRepositoryTest {
    @Test
    fun `maps genres and movie responses from API`() = runTest {
        val api = FakeTmdbApi()
        val repository = TmdbMovieRepository(api = api, hasAccessToken = true)

        val genres = repository.getMovieGenres()
        val movies = repository.discoverMovies(genreId = 28)
        val searchResults = repository.searchMovies("  Dune  ")

        assertEquals("Action", genres.single().name)
        assertEquals("Movie 28", movies.single().title)
        assertEquals(28, api.requestedGenreId)
        assertEquals("Dune", api.searchQuery)
        assertEquals("Search result", searchResults.single().title)
    }

    @Test
    fun `missing token fails before any API request`() = runTest {
        val api = FakeTmdbApi()
        val repository = TmdbMovieRepository(api = api, hasAccessToken = false)

        try {
            repository.discoverMovies()
            fail("Expected MissingTmdbTokenException")
        } catch (_: MissingTmdbTokenException) {
            // Expected: credentials are validated before the API is called.
        }
        assertEquals(0, api.callCount)
    }

    @Test
    fun `maps movie detail from API`() = runTest {
        val api = FakeTmdbApi()
        val repository = TmdbMovieRepository(api = api, hasAccessToken = true)

        val detail = repository.getMovieDetail(550)

        assertEquals(550, detail.id)
        assertEquals("Fight Club", detail.title)
        assertEquals(2, detail.genres.size)
        assertEquals("Drama", detail.genres[0].name)
        assertEquals(550, api.requestedMovieId)
    }

    private class FakeTmdbApi : TmdbApi {
        var callCount = 0
        var requestedGenreId: Int? = null
        var searchQuery: String? = null
        var requestedMovieId: Int? = null

        override suspend fun getMovieGenres(language: String): GenreListDto {
            callCount += 1
            return GenreListDto(listOf(GenreDto(28, "Action")))
        }

        override suspend fun discoverMovies(
            language: String,
            includeAdult: Boolean,
            includeVideo: Boolean,
            sortBy: String,
            genreId: Int?,
            page: Int,
        ): MoviePageDto {
            callCount += 1
            requestedGenreId = genreId
            return moviePage("Movie $genreId")
        }

        override suspend fun searchMovies(
            query: String,
            language: String,
            includeAdult: Boolean,
            page: Int,
        ): MoviePageDto {
            callCount += 1
            searchQuery = query
            return moviePage("Search result")
        }

        override suspend fun getMovieDetails(movieId: Int, language: String): MovieDetailDto {
            callCount += 1
            requestedMovieId = movieId
            return MovieDetailDto(
                id = movieId,
                title = "Fight Club",
                posterPath = "/poster.jpg",
                overview = "An insomniac office worker...",
                releaseDate = "1999-10-15",
                voteAverage = 8.4,
                genres = listOf(
                    GenreDto(18, "Drama"),
                    GenreDto(53, "Thriller"),
                ),
            )
        }

        override suspend fun getTrendingMovies(language: String): MoviePageDto {
            callCount += 1
            return moviePage("Trending")
        }

        override suspend fun getNowPlayingMovies(language: String, page: Int): MoviePageDto {
            callCount += 1
            return moviePage("Now Playing")
        }

        override suspend fun getTopRatedMovies(language: String, page: Int): MoviePageDto {
            callCount += 1
            return moviePage("Top Rated")
        }

        override suspend fun getPopularTvShows(
            language: String,
            page: Int,
        ): TvShowPageDto = TvShowPageDto(
            page = 1,
            results = emptyList(),
            totalPages = 1,
            totalResults = 0,
        )

        override suspend fun searchTvShows(
            query: String,
            language: String,
            page: Int,
        ): TvShowPageDto = TvShowPageDto(
            page = 1,
            results = emptyList(),
            totalPages = 1,
            totalResults = 0,
        )

        override suspend fun getTvShowDetails(
            tvShowId: Int,
            language: String,
        ): TvShowDetailDto = TvShowDetailDto(
            id = tvShowId,
            name = "Test Show",
            posterPath = null,
            backdropPath = null,
            overview = "",
            firstAirDate = "2024-01-01",
            voteAverage = 7.0,
            genres = emptyList(),
            numberOfSeasons = 1,
            numberOfEpisodes = 10,
            status = "Returning Series",
        )

        override suspend fun getTvGenres(language: String): GenreListDto = GenreListDto(emptyList())

        override suspend fun getTvSeason(
            tvShowId: Int,
            seasonNumber: Int,
            language: String,
        ): SeasonDto = SeasonDto(
            id = tvShowId * 100 + seasonNumber,
            seasonNumber = seasonNumber,
            name = "Season $seasonNumber",
            posterPath = null,
            episodeCount = 0,
            episodes = emptyList(),
        )

        private fun moviePage(title: String) = MoviePageDto(
            page = 1,
            results = listOf(
                MovieDto(
                    id = 1,
                    title = title,
                    posterPath = null,
                    overview = "",
                    releaseDate = "",
                    voteAverage = 7.0,
                    genreIds = emptyList(),
                ),
            ),
            totalPages = 1,
            totalResults = 1,
        )
    }
}
