package com.kastlg.app.presentation.tvdetail

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.data.remote.flixcorn.FlixcornError
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.domain.models.Episode
import com.kastlg.app.domain.models.FavoriteMovie
import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.Season
import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.models.TvShowDetail
import com.kastlg.app.domain.repositories.FavoriteRepository
import com.kastlg.app.domain.repositories.FlixcornRepository
import com.kastlg.app.domain.repositories.MovieRepository
import com.kastlg.app.domain.usecases.GetTvSeasonUseCase
import com.kastlg.app.domain.usecases.GetTvShowDetailUseCase
import com.kastlg.app.domain.usecases.ResolveFlixcornSeriesSlugUseCase
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
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
class TvShowDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `episode tap resolves slug and emits navigation event`() = runTest {
        val navigationEvents = mutableListOf<TvShowDetailViewModel.NavigationEvent>()
        val flixcornRepository = FakeFlixcornRepository(
            results = listOf(searchResult("Beastars", "bestias-divinas")),
        )
        val viewModel = createViewModel(flixcornRepository = flixcornRepository)
        runCurrent()

        backgroundScope.launch {
            viewModel.navigationEvent.collect { navigationEvents.add(it) }
        }

        viewModel.selectEpisode(episode(season = 1, episodeNumber = 1))
        runCurrent()

        assertEquals("Beastars", flixcornRepository.lastQuery)
        assertEquals(1, navigationEvents.size)
        val event = navigationEvents[0] as TvShowDetailViewModel.NavigationEvent.NavigateToFlixcornEpisode
        assertEquals("bestias-divinas", event.slug)
        assertEquals(1, event.season)
        assertEquals(1, event.episode)
        assertFalse(viewModel.uiState.value.isResolvingEpisode)
        assertNull(viewModel.uiState.value.episodeNotice)
    }

    @Test
    fun `episode tap shows resolving state while search is in flight`() = runTest {
        val navigationEvents = mutableListOf<TvShowDetailViewModel.NavigationEvent>()
        val gatedRepository = GatedFlixcornRepository(
            results = listOf(searchResult("Beastars", "bestias-divinas")),
        )
        val viewModel = createViewModel(flixcornRepository = gatedRepository)
        runCurrent()

        backgroundScope.launch {
            viewModel.navigationEvent.collect { navigationEvents.add(it) }
        }

        viewModel.selectEpisode(episode(season = 2, episodeNumber = 3))
        runCurrent()

        assertTrue(viewModel.uiState.value.isResolvingEpisode)
        assertNull(viewModel.uiState.value.episodeNotice)
        assertTrue(navigationEvents.isEmpty())

        gatedRepository.gate.complete(Unit)
        runCurrent()

        assertFalse(viewModel.uiState.value.isResolvingEpisode)
        assertEquals(1, navigationEvents.size)
        val event = navigationEvents[0] as TvShowDetailViewModel.NavigationEvent.NavigateToFlixcornEpisode
        assertEquals("bestias-divinas", event.slug)
        assertEquals(2, event.season)
        assertEquals(3, event.episode)
    }

    @Test
    fun `episode tap with no match shows notice and no navigation`() = runTest {
        val navigationEvents = mutableListOf<TvShowDetailViewModel.NavigationEvent>()
        val viewModel = createViewModel(flixcornRepository = FakeFlixcornRepository())
        runCurrent()

        backgroundScope.launch {
            viewModel.navigationEvent.collect { navigationEvents.add(it) }
        }

        viewModel.selectEpisode(episode(season = 1, episodeNumber = 1))
        runCurrent()

        assertFalse(viewModel.uiState.value.isResolvingEpisode)
        assertTrue(viewModel.uiState.value.episodeNotice.orEmpty().contains("Flixcorn"))
        assertTrue(navigationEvents.isEmpty())
    }

    @Test
    fun `episode tap with search error shows notice and no navigation`() = runTest {
        val navigationEvents = mutableListOf<TvShowDetailViewModel.NavigationEvent>()
        val viewModel = createViewModel(
            flixcornRepository = FakeFlixcornRepository(error = FlixcornError.NETWORK_TIMEOUT),
        )
        runCurrent()

        backgroundScope.launch {
            viewModel.navigationEvent.collect { navigationEvents.add(it) }
        }

        viewModel.selectEpisode(episode(season = 1, episodeNumber = 1))
        runCurrent()

        assertFalse(viewModel.uiState.value.isResolvingEpisode)
        assertTrue(viewModel.uiState.value.episodeNotice.orEmpty().contains("Intenta de nuevo"))
        assertTrue(navigationEvents.isEmpty())
    }

    private fun createViewModel(
        flixcornRepository: FlixcornRepository = FakeFlixcornRepository(),
    ) = TvShowDetailViewModel(
        tvShowId = 100,
        getTvShowDetail = GetTvShowDetailUseCase(FakeDetailRepository()),
        getTvSeason = GetTvSeasonUseCase(FakeDetailRepository()),
        favoriteRepository = FakeFavoriteRepository(),
        resolveFlixcornSeriesSlug = ResolveFlixcornSeriesSlugUseCase(flixcornRepository),
    )

    private fun episode(season: Int, episodeNumber: Int) = Episode(
        id = season * 100 + episodeNumber,
        episodeNumber = episodeNumber,
        name = "Episodio $episodeNumber",
        overview = "Resumen",
        airDate = "2020-01-01",
        stillUrl = null,
        seasonNumber = season,
    )

    private fun searchResult(title: String, slug: String) = FlixcornSearchResult(
        title = title,
        slug = slug,
        year = null,
        posterUrl = null,
        genres = emptyList(),
    )

    private class FakeFavoriteRepository : FavoriteRepository {
        private val favorites = MutableStateFlow<Map<Int, TvShowDetail>>(emptyMap())

        override fun observeFavorites(): Flow<List<FavoriteMovie>> =
            flow { emit(emptyList()) }

        override fun observeIsFavorite(tmdbId: Int): Flow<Boolean> =
            favorites.map { tmdbId in it }

        override suspend fun toggle(movie: com.kastlg.app.domain.models.MovieDetail) = Unit

        override suspend fun toggleTvShow(tvShow: TvShowDetail) {
            favorites.value = favorites.value.toMutableMap().apply {
                if (containsKey(tvShow.id)) remove(tvShow.id) else put(tvShow.id, tvShow)
            }
        }
    }

    private class FakeDetailRepository : MovieRepository {
        override suspend fun getMovieGenres(): List<Genre> = emptyList()
        override suspend fun getTvGenres(): List<Genre> = emptyList()
        override suspend fun discoverMovies(genreId: Int?): List<Movie> = emptyList()
        override suspend fun searchMovies(query: String): List<Movie> = emptyList()

        override suspend fun getMovieDetail(movieId: Int): com.kastlg.app.domain.models.MovieDetail =
            error("Not needed in tv detail tests")

        override suspend fun getTrendingMovies(): List<Movie> = emptyList()
        override suspend fun getNowPlayingMovies(): List<Movie> = emptyList()
        override suspend fun getTopRatedMovies(): List<Movie> = emptyList()
        override suspend fun getPopularTvShows(): List<TvShow> = emptyList()
        override suspend fun searchTvShows(query: String): List<TvShow> = emptyList()

        override suspend fun getTvShowDetail(tvShowId: Int): TvShowDetail = TvShowDetail(
            id = tvShowId,
            title = "Beastars",
            posterUrl = null,
            backdropUrl = null,
            overview = "Un lobo en un instituto de herbívoros.",
            releaseDate = "2019-10-10",
            voteAverage = 8.0,
            genres = listOf(Genre(16, "Animación")),
            numberOfSeasons = 3,
            numberOfEpisodes = 36,
            status = "Finalizada",
        )

        override suspend fun getTvSeason(tvShowId: Int, seasonNumber: Int): Season = Season(
            id = tvShowId * 100 + seasonNumber,
            seasonNumber = seasonNumber,
            name = "Temporada $seasonNumber",
            posterUrl = null,
            episodeCount = 12,
            episodes = emptyList(),
        )
    }

    private open class FakeFlixcornRepository(
        private val results: List<FlixcornSearchResult> = emptyList(),
        private val error: FlixcornError? = null,
    ) : FlixcornRepository {
        var lastQuery: String? = null

        override suspend fun searchSeries(query: String): FlixcornResult<List<FlixcornSearchResult>> {
            lastQuery = query
            return if (error != null) FlixcornResult.Error(error) else FlixcornResult.Success(results)
        }

        override suspend fun getSeriesDetail(slug: String): FlixcornResult<FlixcornSeriesDetail> =
            FlixcornResult.Error(FlixcornError.PARSE_FAILURE)

        override suspend fun getEpisodeServers(
            slug: String,
            season: Int,
            episode: Int,
        ): FlixcornResult<List<StreamingServer>> = FlixcornResult.Error(FlixcornError.NO_SERVERS_FOUND)

        override suspend fun resolvePlayerUrl(token: String): FlixcornResult<String> =
            FlixcornResult.Success("https://example.com/resolve/$token")
    }

    private class GatedFlixcornRepository(
        private val results: List<FlixcornSearchResult>,
    ) : FakeFlixcornRepository(results) {
        val gate = CompletableDeferred<Unit>()

        override suspend fun searchSeries(query: String): FlixcornResult<List<FlixcornSearchResult>> {
            lastQuery = query
            gate.await()
            return FlixcornResult.Success(results)
        }
    }
}
