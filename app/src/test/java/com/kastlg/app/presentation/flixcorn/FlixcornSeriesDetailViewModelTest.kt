package com.kastlg.app.presentation.flixcorn

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.data.remote.flixcorn.FlixcornError
import com.kastlg.app.data.remote.flixcorn.FlixcornEpisode
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeason
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.domain.repositories.FlixcornRepository
import com.kastlg.app.domain.usecases.GetFlixcornSeriesDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlixcornSeriesDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads series detail successfully and clears loading`() = runTest {
        val repository = FakeFlixcornRepository(
            seriesResults = listOf(FlixcornResult.Success(seriesDetail())),
        )
        val viewModel = createViewModel(repository)
        viewModel.loadSeries()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Test Series", state.series?.title)
        assertEquals(1, repository.seriesCallCount)
    }

    @Test
    fun `load failure sets error and retry recovers`() = runTest {
        val repository = FakeFlixcornRepository(
            seriesResults = listOf(
                FlixcornResult.Error(FlixcornError.UNREACHABLE),
                FlixcornResult.Success(seriesDetail()),
            ),
        )
        val viewModel = createViewModel(repository)
        viewModel.loadSeries()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertEquals(1, repository.seriesCallCount)

        viewModel.loadSeries()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals("Test Series", viewModel.uiState.value.series?.title)
        assertEquals(2, repository.seriesCallCount)
    }

    private fun createViewModel(
        repository: FlixcornRepository = FakeFlixcornRepository(),
        slug: String = "test-slug",
    ) = FlixcornSeriesDetailViewModel(
        slug = slug,
        getSeriesDetail = GetFlixcornSeriesDetail(repository),
    )

    private fun seriesDetail() = FlixcornSeriesDetail(
        slug = "test-slug",
        title = "Test Series",
        posterUrl = null,
        backdropUrl = null,
        overview = "A test series overview.",
        year = 2020,
        rating = 8.5,
        genres = listOf("Drama"),
        numberOfSeasons = 1,
        numberOfEpisodes = 1,
        status = "En emisión",
        seasons = listOf(
            FlixcornSeason(
                seasonNumber = 1,
                episodes = listOf(
                    FlixcornEpisode(
                        episodeNumber = 1,
                        title = "Pilot",
                        synopsis = "First episode.",
                        seasonNumber = 1,
                    ),
                ),
            ),
        ),
    )

    private class FakeFlixcornRepository(
        private val seriesResults: List<FlixcornResult<FlixcornSeriesDetail>> = emptyList(),
    ) : FlixcornRepository {
        private val seriesQueue = ArrayDeque(seriesResults)
        var seriesCallCount = 0

        override suspend fun searchSeries(query: String): FlixcornResult<List<FlixcornSearchResult>> =
            FlixcornResult.Success(emptyList())

        override suspend fun getSeriesDetail(slug: String): FlixcornResult<FlixcornSeriesDetail> {
            seriesCallCount += 1
            return seriesQueue.removeFirst()
        }

        override suspend fun getEpisodeServers(
            slug: String,
            season: Int,
            episode: Int,
        ): FlixcornResult<List<StreamingServer>> =
            FlixcornResult.Success(emptyList())

        override suspend fun resolvePlayerUrl(token: String): FlixcornResult<String> =
            FlixcornResult.Success("https://example.com/resolve/$token")
    }
}