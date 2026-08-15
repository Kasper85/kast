package com.kastlg.app.presentation.flixcorn

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.data.remote.flixcorn.FlixcornEpisode
import com.kastlg.app.data.remote.flixcorn.FlixcornError
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeason
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.domain.repositories.FlixcornRepository
import com.kastlg.app.domain.repositories.WatchedRepository
import com.kastlg.app.domain.usecases.GetFlixcornEpisodeServers
import com.kastlg.app.domain.usecases.GetFlixcornSeriesDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlixcornEpisodeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads servers successfully and clears loading`() = runTest {
        val repository = FakeFlixcornRepository(
            episodeResults = listOf(
                FlixcornResult.Success(listOf(server("Server A"), server("Server B"))),
            ),
        )
        val viewModel = createViewModel(repository)
        viewModel.loadServers()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.servers.size)
        assertEquals(listOf("Server A", "Server B"), state.servers.map { it.serverName })
        assertEquals(1, repository.episodeCallCount)
    }

    @Test
    fun `load failure sets error and retry recovers`() = runTest {
        val repository = FakeFlixcornRepository(
            episodeResults = listOf(
                FlixcornResult.Error(FlixcornError.UNREACHABLE),
                FlixcornResult.Success(listOf(server("Server A"))),
            ),
        )
        val viewModel = createViewModel(repository)
        viewModel.loadServers()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertEquals(1, repository.episodeCallCount)

        viewModel.loadServers()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(listOf("Server A"), viewModel.uiState.value.servers.map { it.serverName })
        assertEquals(2, repository.episodeCallCount)
    }

    @Test
    fun `empty servers settles with loading cleared`() = runTest {
        val repository = FakeFlixcornRepository(
            episodeResults = listOf(FlixcornResult.Success(emptyList())),
        )
        val viewModel = createViewModel(repository)
        viewModel.loadServers()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.servers.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `re-entry settles with cached servers and loading cleared`() = runTest {
        val repository = FakeFlixcornRepository(
            episodeResults = listOf(
                FlixcornResult.Success(listOf(server("Cached"))),
                FlixcornResult.Success(listOf(server("Cached"))),
            ),
        )
        val viewModel = createViewModel(repository)
        viewModel.loadServers()
        runCurrent()

        assertEquals(listOf("Cached"), viewModel.uiState.value.servers.map { it.serverName })
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadServers()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("Cached"), viewModel.uiState.value.servers.map { it.serverName })
        assertEquals(2, repository.episodeCallCount)
    }

    @Test
    fun `selectedLanguage survives server load`() = runTest {
        val repository = FakeFlixcornRepository(
            episodeResults = listOf(
                FlixcornResult.Success(listOf(server("Server A"))),
                FlixcornResult.Success(listOf(server("Server A"))),
            ),
        )
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.selectLanguage("Español")
        assertEquals("Español", viewModel.uiState.value.selectedLanguage)

        viewModel.loadServers()
        runCurrent()

        assertEquals("Español", viewModel.uiState.value.selectedLanguage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("Server A"), viewModel.uiState.value.servers.map { it.serverName })
    }

    @Test
    fun `next episode within same season points to episode plus one`() = runTest {
        val repository = FakeFlixcornRepository(
            seriesDetailResults = listOf(
                FlixcornResult.Success(
                    seriesDetail(episodeCounts = mapOf(1 to 3, 2 to 3), numberOfSeasons = 2),
                ),
            ),
        )
        val viewModel = createViewModel(repository, season = 1, episode = 1)
        runCurrent()

        assertEquals(1 to 2, viewModel.uiState.value.nextEpisode)
    }

    @Test
    fun `next episode crosses season at season boundary`() = runTest {
        val repository = FakeFlixcornRepository(
            seriesDetailResults = listOf(
                FlixcornResult.Success(
                    seriesDetail(episodeCounts = mapOf(1 to 3, 2 to 3), numberOfSeasons = 2),
                ),
            ),
        )
        val viewModel = createViewModel(repository, season = 1, episode = 3)
        runCurrent()

        assertEquals(2 to 1, viewModel.uiState.value.nextEpisode)
    }

    @Test
    fun `next episode hidden at end of last season`() = runTest {
        val repository = FakeFlixcornRepository(
            seriesDetailResults = listOf(
                FlixcornResult.Success(
                    seriesDetail(episodeCounts = mapOf(1 to 3, 2 to 3), numberOfSeasons = 2),
                ),
            ),
        )
        val viewModel = createViewModel(repository, season = 2, episode = 3)
        runCurrent()

        assertNull(viewModel.uiState.value.nextEpisode)
    }

    @Test
    fun `loadNextEpisode loads next episode servers and advances position`() = runTest {
        val repository = FakeFlixcornRepository(
            seriesDetailResults = listOf(
                FlixcornResult.Success(
                    seriesDetail(episodeCounts = mapOf(1 to 3), numberOfSeasons = 1),
                ),
            ),
            episodeResults = listOf(
                FlixcornResult.Success(listOf(server("S1E2 server"))),
            ),
        )
        val viewModel = createViewModel(repository, season = 1, episode = 1)
        runCurrent()
        assertEquals(1 to 2, viewModel.uiState.value.nextEpisode)

        viewModel.loadNextEpisode()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("S1E2 server"), viewModel.uiState.value.servers.map { it.serverName })
        assertEquals(Triple("test-slug", 1, 2), repository.episodeRequests.last())
        // Position advanced, so next is now S1E3.
        assertEquals(1 to 3, viewModel.uiState.value.nextEpisode)
    }

    @Test
    fun `next episode load failure shows error and retry recovers`() = runTest {
        val repository = FakeFlixcornRepository(
            seriesDetailResults = listOf(
                FlixcornResult.Success(
                    seriesDetail(episodeCounts = mapOf(1 to 3), numberOfSeasons = 1),
                ),
            ),
            episodeResults = listOf(
                FlixcornResult.Error(FlixcornError.UNREACHABLE),
                FlixcornResult.Success(listOf(server("Retried server"))),
            ),
        )
        val viewModel = createViewModel(repository, season = 1, episode = 1)
        runCurrent()

        viewModel.loadNextEpisode()
        runCurrent()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(1, repository.episodeCallCount)

        viewModel.loadNextEpisode()
        runCurrent()

        assertNull(viewModel.uiState.value.error)
        assertEquals(listOf("Retried server"), viewModel.uiState.value.servers.map { it.serverName })
        assertEquals(2, repository.episodeCallCount)
    }

    @Test
    fun `isWatched observes episode watched state`() = runTest {
        val watchedRepository = FakeWatchedRepository()
        val viewModel = createViewModel(
            repository = FakeFlixcornRepository(),
            watchedRepository = watchedRepository,
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isWatched)

        watchedRepository.setEpisodeWatched("test-slug", 1, 1, watched = true)
        runCurrent()
        assertTrue(viewModel.uiState.value.isWatched)

        watchedRepository.setEpisodeWatched("test-slug", 1, 1, watched = false)
        runCurrent()
        assertFalse(viewModel.uiState.value.isWatched)
    }

    @Test
    fun `toggleWatched flips episode watched state`() = runTest {
        val watchedRepository = FakeWatchedRepository()
        val viewModel = createViewModel(
            repository = FakeFlixcornRepository(),
            watchedRepository = watchedRepository,
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isWatched)

        viewModel.toggleWatched()
        runCurrent()
        assertTrue(viewModel.uiState.value.isWatched)
        assertEquals(Triple("test-slug", 1, 1), watchedRepository.lastToggledEpisode)

        viewModel.toggleWatched()
        runCurrent()
        assertFalse(viewModel.uiState.value.isWatched)
    }

    private fun createViewModel(
        repository: FlixcornRepository = FakeFlixcornRepository(),
        slug: String = "test-slug",
        season: Int = 1,
        episode: Int = 1,
        watchedRepository: WatchedRepository = FakeWatchedRepository(),
    ) = FlixcornEpisodeViewModel(
        slug = slug,
        season = season,
        episode = episode,
        getEpisodeServers = GetFlixcornEpisodeServers(repository),
        getSeriesDetail = GetFlixcornSeriesDetail(repository),
        watchedRepository = watchedRepository,
    )

    private fun server(name: String) = StreamingServer(
        serverName = name,
        quality = "1080p",
        language = "Español",
        onlineUrl = "https://example.com/$name",
        directUrl = null,
        serverIconUrl = null,
    )

    private fun seriesDetail(
        episodeCounts: Map<Int, Int>,
        numberOfSeasons: Int,
    ) = FlixcornSeriesDetail(
        slug = "test-slug",
        title = "Test Series",
        posterUrl = null,
        backdropUrl = null,
        overview = "",
        year = null,
        rating = null,
        genres = emptyList(),
        numberOfSeasons = numberOfSeasons,
        numberOfEpisodes = episodeCounts.values.sum(),
        status = "",
        seasons = episodeCounts.map { (seasonNumber, count) ->
            FlixcornSeason(
                seasonNumber = seasonNumber,
                episodes = (1..count).map { episodeNumber ->
                    FlixcornEpisode(
                        episodeNumber = episodeNumber,
                        title = "S${seasonNumber}E$episodeNumber",
                        synopsis = "",
                        seasonNumber = seasonNumber,
                    )
                },
            )
        },
    )

    private class FakeFlixcornRepository(
        private val episodeResults: List<FlixcornResult<List<StreamingServer>>> = emptyList(),
        private val seriesDetailResults: List<FlixcornResult<FlixcornSeriesDetail>> = emptyList(),
    ) : FlixcornRepository {
        private val episodeQueue = ArrayDeque(episodeResults)
        private val seriesDetailQueue = ArrayDeque(seriesDetailResults)
        var episodeCallCount = 0
        val episodeRequests = mutableListOf<Triple<String, Int, Int>>()

        override suspend fun searchSeries(query: String): FlixcornResult<List<FlixcornSearchResult>> =
            FlixcornResult.Success(emptyList())

        override suspend fun getSeriesDetail(slug: String): FlixcornResult<FlixcornSeriesDetail> =
            if (seriesDetailQueue.isEmpty()) {
                FlixcornResult.Error(FlixcornError.NO_SERVERS_FOUND)
            } else {
                seriesDetailQueue.removeFirst()
            }

        override suspend fun getEpisodeServers(
            slug: String,
            season: Int,
            episode: Int,
        ): FlixcornResult<List<StreamingServer>> {
            episodeCallCount += 1
            episodeRequests += Triple(slug, season, episode)
            return episodeQueue.removeFirst()
        }

        override suspend fun resolvePlayerUrl(token: String): FlixcornResult<String> =
            FlixcornResult.Success("https://example.com/resolve/$token")
    }

    private class FakeWatchedRepository : WatchedRepository {
        private val episodeStates = MutableStateFlow<Map<Triple<String, Int, Int>, Boolean>>(emptyMap())
        private val movieStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
        var lastToggledEpisode: Triple<String, Int, Int>? = null

        override fun observeIsMovieWatched(movieId: Int): Flow<Boolean> =
            movieStates.map { it[movieId] ?: false }

        override fun observeIsEpisodeWatched(slug: String, season: Int, episode: Int): Flow<Boolean> =
            episodeStates.map { it[Triple(slug, season, episode)] ?: false }

        override suspend fun toggleMovie(movieId: Int) {
            movieStates.value = movieStates.value.toMutableMap().apply {
                put(movieId, !(get(movieId) ?: false))
            }
        }

        override suspend fun toggleEpisode(slug: String, season: Int, episode: Int) {
            val key = Triple(slug, season, episode)
            lastToggledEpisode = key
            episodeStates.value = episodeStates.value.toMutableMap().apply {
                put(key, !(get(key) ?: false))
            }
        }

        fun setEpisodeWatched(slug: String, season: Int, episode: Int, watched: Boolean) {
            val key = Triple(slug, season, episode)
            episodeStates.value = episodeStates.value.toMutableMap().apply {
                if (watched) put(key, true) else remove(key)
            }
        }
    }
}
