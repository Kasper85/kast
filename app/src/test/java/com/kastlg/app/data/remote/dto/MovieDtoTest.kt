package com.kastlg.app.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDtoTest {
    @Test
    fun `maps TMDB movie fields and poster URL`() {
        val movie = MovieDto(
            id = 42,
            title = "The Answer",
            posterPath = "/poster.jpg",
            overview = "Overview",
            releaseDate = "2026-06-18",
            voteAverage = 8.25,
            genreIds = listOf(12, 878),
        ).toDomain()

        assertEquals(42, movie.id)
        assertEquals("The Answer", movie.title)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", movie.posterUrl)
        assertEquals("2026-06-18", movie.releaseDate)
        assertEquals(listOf(12, 878), movie.genreIds)
    }

    @Test
    fun `keeps missing poster and release date safe`() {
        val movie = MovieDto(
            id = 7,
            title = "Unknown",
            posterPath = null,
            overview = "",
            releaseDate = null,
            voteAverage = 0.0,
            genreIds = emptyList(),
        ).toDomain()

        assertNull(movie.posterUrl)
        assertEquals("", movie.releaseDate)
    }
}
