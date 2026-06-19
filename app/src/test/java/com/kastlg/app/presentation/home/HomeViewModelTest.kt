package com.kastlg.app.presentation.home

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.models.TvShowDetail
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import com.kastlg.app.domain.repositories.MovieRepository
import com.kastlg.app.domain.usecases.GetMovieGenresUseCase
import com.kastlg.app.domain.usecases.GetNowPlayingMoviesUseCase
import com.kastlg.app.domain.usecases.GetPopularTvShowsUseCase
import com.kastlg.app.domain.usecases.GetTopRatedMoviesUseCase
import com.kastlg.app.domain.usecases.GetTrendingMoviesUseCase
import com.kastlg.app.domain.usecases.GetTvGenresUseCase
import com.kastlg.app.domain.usecases.SearchMoviesUseCase
import com.kastlg.app.domain.usecases.SearchTvShowsUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads all categories initially`() = runTest {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        runCurrent()

        assertEquals("Trending", viewModel.uiState.value.trendingMovies.single().title)
        assertEquals("Now Playing", viewModel.uiState.value.nowPlayingMovies.single().title)
        assertEquals("Top Rated", viewModel.uiState.value.topRatedMovies.single().title)
        assertTrue(viewModel.uiState.value.popularTvShows.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `search is debounced and executes after delay`() = runTest {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        advanceTimeBy(400)
        runCurrent()

        // Start typing — search should NOT fire yet (debounce)
        viewModel.onQueryChange("Dune")
        runCurrent()
        assertTrue("Search should not fire before debounce", repository.searchQueries.isEmpty())

        // After debounce fires, search executes
        advanceTimeBy(400)
        runCurrent()
        assertEquals(listOf("Dune"), repository.searchQueries)
    }

    @Test
    fun `empty query reloads categories`() = runTest {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.onQueryChange("Dune")
        advanceTimeBy(400)
        runCurrent()
        assertEquals("Dune", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.trendingMovies.isEmpty())

        viewModel.onQueryChange("")
        runCurrent()
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals("Trending", viewModel.uiState.value.trendingMovies.single().title)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun `missing token produces empty categories without crashing`() = runTest {
        val repository = FakeMovieRepository(missingToken = true)
        val viewModel = createViewModel(repository)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.trendingMovies.isEmpty())
        assertTrue(state.nowPlayingMovies.isEmpty())
        assertTrue(state.topRatedMovies.isEmpty())
        assertTrue(state.popularTvShows.isEmpty())
    }

    @Test
    fun `load failure can be retried successfully`() = runTest {
        val repository = FakeMovieRepository(failuresBeforeSuccess = 1)
        val viewModel = createViewModel(repository)
        runCurrent()

        assertTrue(viewModel.uiState.value.trendingMovies.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.retry()
        runCurrent()

        assertEquals(2, repository.requestCount)
        assertEquals("Trending", viewModel.uiState.value.trendingMovies.single().title)
    }

    private fun createViewModel(repository: MovieRepository) = HomeViewModel(
        getTrendingMovies = GetTrendingMoviesUseCase(repository),
        getNowPlayingMovies = GetNowPlayingMoviesUseCase(repository),
        getTopRatedMovies = GetTopRatedMoviesUseCase(repository),
        getPopularTvShows = GetPopularTvShowsUseCase(repository),
        searchMovies = SearchMoviesUseCase(repository),
        searchTvShows = SearchTvShowsUseCase(repository),
        getMovieGenres = GetMovieGenresUseCase(repository),
        getTvGenres = GetTvGenresUseCase(repository),
    )

    private class FakeMovieRepository(
        private val missingToken: Boolean = false,
        private val failuresBeforeSuccess: Int = 0,
    ) : MovieRepository {
        val searchQueries = mutableListOf<String>()
        val cancelledQueries = mutableListOf<String>()
        var requestCount = 0

        override suspend fun getMovieGenres(): List<Genre> = emptyList()

        override suspend fun getTvGenres(): List<Genre> = emptyList()

        override suspend fun discoverMovies(genreId: Int?): List<Movie> = emptyList()

        override suspend fun searchMovies(query: String): List<Movie> {
            checkToken()
            searchQueries += query
            if (query == "Dune") {
                try {
                    delay(10_000)
                } finally {
                    cancelledQueries += query
                }
            }
            return listOf(movie(query))
        }

        override suspend fun getMovieDetail(movieId: Int): MovieDetail = error("Not needed")

        override suspend fun getTrendingMovies(): List<Movie> {
            checkToken()
            requestCount += 1
            if (requestCount <= failuresBeforeSuccess) {
                throw IOException("Load failed")
            }
            return listOf(movie("Trending"))
        }

        override suspend fun getNowPlayingMovies(): List<Movie> {
            checkToken()
            return listOf(movie("Now Playing"))
        }

        override suspend fun getTopRatedMovies(): List<Movie> {
            checkToken()
            return listOf(movie("Top Rated"))
        }

        override suspend fun getPopularTvShows(): List<TvShow> = emptyList()

        override suspend fun searchTvShows(query: String): List<TvShow> = emptyList()

        override suspend fun getTvShowDetail(tvShowId: Int): TvShowDetail = error("Not needed")

        override suspend fun getTvSeason(tvShowId: Int, seasonNumber: Int): com.kastlg.app.domain.models.Season =
            com.kastlg.app.domain.models.Season(
                id = tvShowId * 100 + seasonNumber,
                seasonNumber = seasonNumber,
                name = "Season $seasonNumber",
                posterUrl = null,
                episodeCount = 0,
                episodes = emptyList(),
            )

        private fun checkToken() {
            if (missingToken) throw MissingTmdbTokenException()
        }

        private fun movie(title: String) = Movie(
            id = title.hashCode(),
            title = title,
            posterUrl = null,
            overview = "",
            releaseDate = "",
            voteAverage = 0.0,
            genreIds = emptyList(),
        )
    }
}
