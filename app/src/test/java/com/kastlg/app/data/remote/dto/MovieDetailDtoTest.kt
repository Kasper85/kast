package com.kastlg.app.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieDetailDtoTest {
    @Test
    fun `maps TMDB movie detail fields and poster URL`() {
        val detail = MovieDetailDto(
            id = 550,
            title = "Fight Club",
            posterPath = "/pB8BM7pdSp6B6Ih7QI4S2t0POD5.jpg",
            overview = "An insomniac office worker and a devil-may-care soap maker...",
            releaseDate = "1999-10-15",
            voteAverage = 8.433,
            genres = listOf(
                GenreDto(18, "Drama"),
                GenreDto(53, "Thriller"),
            ),
        ).toDomain()

        assertEquals(550, detail.id)
        assertEquals("Fight Club", detail.title)
        assertEquals("https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QI4S2t0POD5.jpg", detail.posterUrl)
        assertEquals("1999-10-15", detail.releaseDate)
        assertEquals(8.433, detail.voteAverage, 0.001)
        assertEquals(2, detail.genres.size)
        assertEquals("Drama", detail.genres[0].name)
        assertEquals("Thriller", detail.genres[1].name)
    }

    @Test
    fun `keeps missing poster and release date safe`() {
        val detail = MovieDetailDto(
            id = 1,
            title = "Unknown",
            posterPath = null,
            overview = "",
            releaseDate = null,
            voteAverage = 0.0,
            genres = emptyList(),
        ).toDomain()

        assertNull(detail.posterUrl)
        assertEquals("", detail.releaseDate)
        assertTrue(detail.genres.isEmpty())
    }
}
