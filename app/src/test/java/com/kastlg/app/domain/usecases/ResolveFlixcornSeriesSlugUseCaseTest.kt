package com.kastlg.app.domain.usecases

import com.kastlg.app.data.remote.flixcorn.FlixcornError
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.domain.repositories.FlixcornRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveFlixcornSeriesSlugUseCaseTest {

    @Test
    fun `exact normalized title match returns its slug`() = runTest {
        val repository = FakeFlixcornRepository(
            results = listOf(
                searchResult("Otra Serie", "otra-serie"),
                searchResult("Beastars", "bestias-divinas"),
            ),
        )
        val useCase = ResolveFlixcornSeriesSlugUseCase(repository)

        val result = useCase("Beastars")

        assertTrue(result is FlixcornResult.Success)
        val slug = (result as FlixcornResult.Success).data
        assertEquals("bestias-divinas", slug)
    }

    @Test
    fun `accented query matches unaccented result title`() = runTest {
        val repository = FakeFlixcornRepository(
            results = listOf(
                searchResult("Béastars", "bestias-divinas"),
            ),
        )
        val useCase = ResolveFlixcornSeriesSlugUseCase(repository)

        val result = useCase("Beastars")

        assertTrue(result is FlixcornResult.Success)
        val slug = (result as FlixcornResult.Success).data
        assertEquals("bestias-divinas", slug)
    }

    @Test
    fun `punctuation in query is stripped before matching`() = runTest {
        val repository = FakeFlixcornRepository(
            results = listOf(
                searchResult("Beastars: El Musical", "bestias-divinas-musical"),
            ),
        )
        val useCase = ResolveFlixcornSeriesSlugUseCase(repository)

        val result = useCase("Beastars el musical")

        assertTrue(result is FlixcornResult.Success)
        val slug = (result as FlixcornResult.Success).data
        assertEquals("bestias-divinas-musical", slug)
    }

    @Test
    fun `falls back to first result when no normalized match exists`() = runTest {
        val repository = FakeFlixcornRepository(
            results = listOf(
                searchResult("Otra Serie", "otra-serie"),
                searchResult("Algo Más", "algo-mas"),
            ),
        )
        val useCase = ResolveFlixcornSeriesSlugUseCase(repository)

        val result = useCase("Beastars")

        assertTrue(result is FlixcornResult.Success)
        val slug = (result as FlixcornResult.Success).data
        assertEquals("otra-serie", slug)
    }

    @Test
    fun `empty search results resolve to null slug`() = runTest {
        val repository = FakeFlixcornRepository(results = emptyList())
        val useCase = ResolveFlixcornSeriesSlugUseCase(repository)

        val result = useCase("Serie Inexistente")

        assertTrue(result is FlixcornResult.Success)
        val slug = (result as FlixcornResult.Success).data
        assertNull(slug)
    }

    @Test
    fun `search error is passed through unchanged`() = runTest {
        val repository = FakeFlixcornRepository(error = FlixcornError.NETWORK_TIMEOUT)
        val useCase = ResolveFlixcornSeriesSlugUseCase(repository)

        val result = useCase("Beastars")

        assertTrue(result is FlixcornResult.Error)
        assertEquals(FlixcornError.NETWORK_TIMEOUT, (result as FlixcornResult.Error).code)
    }

    @Test
    fun `forwards the series title as the search query`() = runTest {
        val repository = FakeFlixcornRepository(
            results = listOf(searchResult("Beastars", "bestias-divinas")),
        )
        val useCase = ResolveFlixcornSeriesSlugUseCase(repository)

        useCase("Beastars")

        assertEquals("Beastars", repository.lastQuery)
    }

    private fun searchResult(title: String, slug: String) = FlixcornSearchResult(
        title = title,
        slug = slug,
        year = null,
        posterUrl = null,
        genres = emptyList(),
    )

    private class FakeFlixcornRepository(
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
}
