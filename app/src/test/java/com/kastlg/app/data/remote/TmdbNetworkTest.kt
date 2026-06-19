package com.kastlg.app.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TmdbNetworkTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `production client sends TMDB routes queries and bearer authorization`() = runTest {
        repeat(4) {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        when (it) {
                            0 -> GENRES_RESPONSE
                            3 -> MOVIE_DETAIL_RESPONSE
                            else -> MOVIES_RESPONSE
                        },
                    ),
            )
        }
        val repository = TmdbNetwork.createRepository(
            accessToken = " test-token ",
            baseUrl = server.url("/3/").toString(),
        )

        repository.getMovieGenres()
        repository.discoverMovies(genreId = 28)
        repository.searchMovies("  Dune: Part Two  ")
        repository.getMovieDetail(550)

        val genresRequest = server.takeRequest()
        assertEquals("/3/genre/movie/list?language=en-US", genresRequest.path)
        assertEquals("Bearer test-token", genresRequest.getHeader("Authorization"))
        assertEquals("application/json", genresRequest.getHeader("Accept"))

        val discoverRequest = server.takeRequest()
        assertEquals("/3/discover/movie", discoverRequest.requestUrl?.encodedPath)
        assertEquals("en-US", discoverRequest.requestUrl?.queryParameter("language"))
        assertEquals("false", discoverRequest.requestUrl?.queryParameter("include_adult"))
        assertEquals("false", discoverRequest.requestUrl?.queryParameter("include_video"))
        assertEquals("popularity.desc", discoverRequest.requestUrl?.queryParameter("sort_by"))
        assertEquals("28", discoverRequest.requestUrl?.queryParameter("with_genres"))
        assertEquals("1", discoverRequest.requestUrl?.queryParameter("page"))
        assertEquals("Bearer test-token", discoverRequest.getHeader("Authorization"))

        val searchRequest = server.takeRequest()
        assertEquals("/3/search/movie", searchRequest.requestUrl?.encodedPath)
        assertEquals("Dune: Part Two", searchRequest.requestUrl?.queryParameter("query"))
        assertEquals("en-US", searchRequest.requestUrl?.queryParameter("language"))
        assertEquals("false", searchRequest.requestUrl?.queryParameter("include_adult"))
        assertEquals("1", searchRequest.requestUrl?.queryParameter("page"))
        assertNull(searchRequest.requestUrl?.queryParameter("with_genres"))
        assertEquals("Bearer test-token", searchRequest.getHeader("Authorization"))

        val detailRequest = server.takeRequest()
        assertEquals("/3/movie/550", detailRequest.requestUrl?.encodedPath)
        assertEquals("en-US", detailRequest.requestUrl?.queryParameter("language"))
        assertEquals("Bearer test-token", detailRequest.getHeader("Authorization"))
        assertEquals("application/json", detailRequest.getHeader("Accept"))
    }

    private companion object {
        const val GENRES_RESPONSE = """
            {
              "genres": [
                { "id": 28, "name": "Action" }
              ]
            }
        """

        const val MOVIES_RESPONSE = """
            {
              "page": 1,
              "results": [
                {
                  "id": 1,
                  "title": "Movie",
                  "poster_path": null,
                  "overview": "",
                  "release_date": "2024-01-01",
                  "vote_average": 7.0,
                  "genre_ids": [28]
                }
              ],
              "total_pages": 1,
              "total_results": 1
            }
        """

        const val MOVIE_DETAIL_RESPONSE = """
            {
              "id": 550,
              "title": "Fight Club",
              "poster_path": "/poster.jpg",
              "overview": "An insomniac office worker...",
              "release_date": "1999-10-15",
              "vote_average": 8.4,
              "genres": [
                { "id": 18, "name": "Drama" }
              ]
            }
        """
    }
}
