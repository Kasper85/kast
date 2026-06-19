package com.kastlg.app.presentation.detail

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.data.tv.SsapClient
import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.FavoriteMovie
import com.kastlg.app.domain.models.HistoryEntry
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.models.TvConfig
import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.models.TvShowDetail
import com.kastlg.app.domain.repositories.FavoriteRepository
import com.kastlg.app.domain.repositories.HistoryRepository
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import com.kastlg.app.domain.repositories.MovieRepository
import com.kastlg.app.domain.repositories.TvRepository
import com.kastlg.app.domain.usecases.GetMovieDetailUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads movie detail successfully`() = runTest {
        val repository = FakeDetailRepository()
        val viewModel = createViewModel(movieId = 550, repository = repository)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Fight Club", state.title)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", state.posterUrl)
        assertEquals("1999", state.releaseYear)
        assertEquals(8.4, state.voteAverage, 0.1)
        assertEquals(2, state.genres.size)
        assertEquals("Drama", state.genres[0].name)
        assertNull(state.errorMessage)
        assertEquals(550, repository.requestedMovieId)
    }

    @Test
    fun `missing token shows actionable error`() = runTest {
        val repository = FakeDetailRepository(missingToken = true)
        val viewModel = createViewModel(movieId = 550, repository = repository)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage.orEmpty().contains("Ajustes"))
    }

    @Test
    fun `toggle favorite flips state`() = runTest {
        val repository = FakeDetailRepository()
        val favoriteRepository = FakeFavoriteRepository()
        val viewModel = createViewModel(
            movieId = 550,
            repository = repository,
            favoriteRepository = favoriteRepository,
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isFavorite)
        viewModel.toggleFavorite()
        runCurrent()
        assertTrue(viewModel.uiState.value.isFavorite)
        viewModel.toggleFavorite()
        runCurrent()
        assertFalse(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun `detail failure can be retried successfully`() = runTest {
        val repository = FakeDetailRepository(failuresBeforeSuccess = 1)
        val viewModel = createViewModel(movieId = 550, repository = repository)
        runCurrent()

        assertEquals(1, repository.callCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.errorMessage.orEmpty().contains("conexión a internet"))

        viewModel.retry()
        runCurrent()
        assertEquals(2, repository.callCount)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("Fight Club", viewModel.uiState.value.title)
    }

    @Test
    fun `favorite survives view model recreation`() = runTest {
        val repository = FakeDetailRepository()
        val favoriteRepository = FakeFavoriteRepository()
        val firstViewModel = createViewModel(
            movieId = 550,
            repository = repository,
            favoriteRepository = favoriteRepository,
        )
        runCurrent()

        firstViewModel.toggleFavorite()
        runCurrent()

        val recreatedViewModel = createViewModel(
            movieId = 550,
            repository = repository,
            favoriteRepository = favoriteRepository,
        )
        runCurrent()

        assertTrue(recreatedViewModel.uiState.value.isFavorite)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `successful detail load records deduplicated history through repository`() = runTest {
        val repository = FakeDetailRepository()
        val historyRepository = FakeHistoryRepository()
        val viewModel = createViewModel(
            movieId = 550,
            repository = repository,
            historyRepository = historyRepository,
        )
        runCurrent()

        assertEquals(listOf(550), historyRepository.recordedMovieIds)

        viewModel.retry()
        runCurrent()

        assertEquals(listOf(550, 550), historyRepository.recordedMovieIds)
    }

    @Test
    fun `favorite observation failure is visible without crashing detail`() = runTest {
        val viewModel = createViewModel(
            movieId = 550,
            repository = FakeDetailRepository(),
            favoriteRepository = FakeFavoriteRepository(failObserve = true),
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "No se pudo guardar el favorito. Intenta de nuevo.",
            viewModel.uiState.value.favoriteErrorMessage,
        )
        assertEquals("Fight Club", viewModel.uiState.value.title)
    }

    @Test
    fun `favorite toggle failure is visible and does not escape coroutine`() = runTest {
        val viewModel = createViewModel(
            movieId = 550,
            repository = FakeDetailRepository(),
            favoriteRepository = FakeFavoriteRepository(failToggle = true),
        )
        runCurrent()

        viewModel.toggleFavorite()
        runCurrent()

        assertFalse(viewModel.uiState.value.isFavorite)
        assertEquals(
            "No se pudo guardar el favorito. Intenta de nuevo.",
            viewModel.uiState.value.favoriteErrorMessage,
        )
    }

    @Test
    fun `toggle cancellation after previous error preserves error state`() = runTest {
        val repository = FakeDetailRepository()
        val flakyRepository = FlippyFavoriteRepository(startFailing = true)
        val viewModel = createViewModel(
            movieId = 550,
            repository = repository,
            favoriteRepository = flakyRepository,
        )
        runCurrent()

        // First toggle fails — error should appear
        viewModel.toggleFavorite()
        runCurrent()
        assertEquals(
            "No se pudo guardar el favorito. Intenta de nuevo.",
            viewModel.uiState.value.favoriteErrorMessage,
        )

        // Flip to success and toggle again — error must clear
        flakyRepository.startFailing = false
        viewModel.toggleFavorite()
        runCurrent()
        assertNull(viewModel.uiState.value.favoriteErrorMessage)
        assertTrue(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun `history persistence failure is visible without hiding loaded detail`() = runTest {
        val viewModel = createViewModel(
            movieId = 550,
            repository = FakeDetailRepository(),
            historyRepository = FakeHistoryRepository(failRecord = true),
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Fight Club", viewModel.uiState.value.title)
        assertEquals(
            "No se pudo registrar en el historial.",
            viewModel.uiState.value.historyErrorMessage,
        )
    }

    @Test
    fun `watchOnTv with no TV config navigates to settings`() = runTest {
        val navigationEvents = mutableListOf<MovieDetailViewModel.NavigationEvent>()
        val viewModel = createViewModel(
            movieId = 550,
            repository = FakeDetailRepository(),
            tvRepository = FakeTvRepository(hasConfig = false),
        )
        runCurrent()

        backgroundScope.launch {
            viewModel.navigationEvent.collect { navigationEvents.add(it) }
        }

        viewModel.watchOnTv()
        runCurrent()

        assertEquals(1, navigationEvents.size)
        assertTrue(navigationEvents[0] is MovieDetailViewModel.NavigationEvent.NavigateToTvSettings)
    }

    @Test
    fun `watchOnTv with paired TV opens URL`() = runTest {
        val fakeTvRepository = FakeTvRepository(hasConfig = true, isPaired = true)
        val viewModel = createViewModel(
            movieId = 550,
            repository = FakeDetailRepository(),
            tvRepository = fakeTvRepository,
        )
        runCurrent()

        viewModel.watchOnTv()
        runCurrent()

        assertEquals("https://unlimplay.com/play/embed/movie/550", fakeTvRepository.lastOpenedUrl)
        assertEquals("Enviado a la TV", viewModel.uiState.value.tvSuccessMessage)
        assertNull(viewModel.uiState.value.tvErrorMessage)
    }

    @Test
    fun `watchOnTv with unpaired TV navigates to settings`() = runTest {
        val navigationEvents = mutableListOf<MovieDetailViewModel.NavigationEvent>()
        val viewModel = createViewModel(
            movieId = 550,
            repository = FakeDetailRepository(),
            tvRepository = FakeTvRepository(hasConfig = true, isPaired = false),
        )
        runCurrent()

        backgroundScope.launch {
            viewModel.navigationEvent.collect { navigationEvents.add(it) }
        }

        viewModel.watchOnTv()
        runCurrent()

        assertEquals(1, navigationEvents.size)
        assertTrue(navigationEvents[0] is MovieDetailViewModel.NavigationEvent.NavigateToTvSettings)
    }

    @Test
    fun `watchOnTv failure shows error message`() = runTest {
        val viewModel = createViewModel(
            movieId = 550,
            repository = FakeDetailRepository(),
            tvRepository = FakeTvRepository(shouldFailOpen = true),
        )
        runCurrent()

        viewModel.watchOnTv()
        runCurrent()

        assertEquals("Connection refused", viewModel.uiState.value.tvErrorMessage)
        assertNull(viewModel.uiState.value.tvSuccessMessage)
    }

    private fun createViewModel(
        movieId: Int,
        repository: MovieRepository,
        favoriteRepository: FavoriteRepository = FakeFavoriteRepository(),
        historyRepository: HistoryRepository = FakeHistoryRepository(),
        tvRepository: TvRepository = FakeTvRepository(),
    ) =
        MovieDetailViewModel(
            movieId = movieId,
            getMovieDetail = GetMovieDetailUseCase(repository),
            favoriteRepository = favoriteRepository,
            historyRepository = historyRepository,
            tvRepository = tvRepository,
        )

    private class FakeFavoriteRepository(
        private val failObserve: Boolean = false,
        private val failToggle: Boolean = false,
    ) : FavoriteRepository {
        private val favorites = MutableStateFlow<Map<Int, MovieDetail>>(emptyMap())

        override fun observeFavorites(): Flow<List<FavoriteMovie>> = favorites.map { values ->
            values.values.map {
                FavoriteMovie(
                    tmdbId = it.id,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    overview = it.overview,
                    releaseDate = it.releaseDate,
                    voteAverage = it.voteAverage,
                    favoritedAt = 1L,
                )
            }
        }

        override fun observeIsFavorite(tmdbId: Int): Flow<Boolean> =
            if (failObserve) {
                flow { throw IOException("Favorite observation failed") }
            } else {
                favorites.map { tmdbId in it }
            }

        override suspend fun toggle(movie: MovieDetail) {
            if (failToggle) throw IOException("Favorite toggle failed")
            favorites.value = favorites.value.toMutableMap().apply {
                if (containsKey(movie.id)) remove(movie.id) else put(movie.id, movie)
            }
        }
    }

    private class FakeHistoryRepository(
        private val failRecord: Boolean = false,
    ) : HistoryRepository {
        val recordedMovieIds = mutableListOf<Int>()

        override fun observeHistory(): Flow<List<HistoryEntry>> =
            MutableStateFlow(emptyList())

        override suspend fun recordViewed(movie: MovieDetail) {
            if (failRecord) throw IOException("History write failed")
            recordedMovieIds += movie.id
        }

        override suspend fun recordSentToTv(movie: MovieDetail) {
            if (failRecord) throw IOException("History write failed")
            recordedMovieIds += movie.id
        }
    }

    private class FakeDetailRepository(
        private val missingToken: Boolean = false,
        private var failuresBeforeSuccess: Int = 0,
    ) : MovieRepository {
        var callCount = 0
        var requestedMovieId: Int? = null

        override suspend fun getMovieGenres(): List<Genre> = emptyList()

        override suspend fun getTvGenres(): List<Genre> = emptyList()

        override suspend fun discoverMovies(genreId: Int?): List<Movie> =
            emptyList()

        override suspend fun searchMovies(query: String): List<Movie> =
            emptyList()

        override suspend fun getMovieDetail(movieId: Int): MovieDetail {
            if (missingToken) throw MissingTmdbTokenException()
            callCount += 1
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess -= 1
                throw IOException("Simulated network failure")
            }
            requestedMovieId = movieId
            return MovieDetail(
                id = movieId,
                title = "Fight Club",
                posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                overview = "An insomniac office worker and a devil-may-care soap maker form an underground fight club.",
                releaseDate = "1999-10-15",
                voteAverage = 8.4,
                genres = listOf(Genre(18, "Drama"), Genre(53, "Thriller")),
            )
        }

        override suspend fun getTrendingMovies(): List<Movie> = emptyList()

        override suspend fun getNowPlayingMovies(): List<Movie> = emptyList()

        override suspend fun getTopRatedMovies(): List<Movie> = emptyList()

        override suspend fun getPopularTvShows(): List<TvShow> = emptyList()

        override suspend fun searchTvShows(query: String): List<TvShow> = emptyList()

        override suspend fun getTvShowDetail(tvShowId: Int): TvShowDetail =
            error("Not needed in detail tests")

        override suspend fun getTvSeason(tvShowId: Int, seasonNumber: Int): com.kastlg.app.domain.models.Season =
            com.kastlg.app.domain.models.Season(
                id = tvShowId * 100 + seasonNumber,
                seasonNumber = seasonNumber,
                name = "Season $seasonNumber",
                posterUrl = null,
                episodeCount = 0,
                episodes = emptyList(),
            )
    }

    private class FlippyFavoriteRepository(
        var startFailing: Boolean = false,
    ) : FavoriteRepository {
        private val favorites = MutableStateFlow<Map<Int, MovieDetail>>(emptyMap())

        override fun observeFavorites(): Flow<List<FavoriteMovie>> = favorites.map { values ->
            values.values.map {
                FavoriteMovie(
                    tmdbId = it.id,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    overview = it.overview,
                    releaseDate = it.releaseDate,
                    voteAverage = it.voteAverage,
                    favoritedAt = 1L,
                )
            }
        }

        override fun observeIsFavorite(tmdbId: Int): Flow<Boolean> =
            favorites.map { tmdbId in it }

        override suspend fun toggle(movie: MovieDetail) {
            if (startFailing) throw IOException("Toggle failed")
            favorites.value = favorites.value.toMutableMap().apply {
                if (containsKey(movie.id)) remove(movie.id) else put(movie.id, movie)
            }
        }
    }

    private class FakeTvRepository(
        private val hasConfig: Boolean = true,
        private val isPaired: Boolean = true,
        private val shouldFailOpen: Boolean = false,
    ) : TvRepository {
        var lastOpenedUrl: String? = null

        override fun observeConfig(): Flow<TvConfig?> = MutableStateFlow(
            if (hasConfig) TvConfig("192.168.1.100", "Test TV", "key", isPaired) else null
        )

        override fun observeDiagnosticLog(): Flow<List<SsapClient.DiagnosticEntry>> =
            kotlinx.coroutines.flow.flowOf(emptyList())

        override suspend fun getConfig(): TvConfig? =
            if (hasConfig) TvConfig("192.168.1.100", "Test TV", "key", isPaired) else null

        override suspend fun saveConfig(config: TvConfig) = Unit

        override suspend fun deleteConfig() = Unit

        override suspend fun connectAndRegister(ip: String): Result<String> =
            Result.success("key")

        override suspend fun openUrl(url: String): Result<Unit> {
            if (shouldFailOpen) return Result.failure(Exception("Connection refused"))
            lastOpenedUrl = url
            return Result.success(Unit)
        }

        override suspend fun disconnect() = Unit
    }
}
