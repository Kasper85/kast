package com.kastlg.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kastlg.app.data.local.KastLgDatabase
import com.kastlg.app.domain.models.MovieDetail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomHistoryRepositoryTest {
    private lateinit var database: KastLgDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            KastLgDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `real Room history deduplicates revisits and updates recency`() = runTest {
        var timestamp = 100L
        val repository = RoomHistoryRepository(database.historyDao()) { timestamp }

        repository.recordViewed(movie(1, "First"))
        timestamp = 200L
        repository.recordViewed(movie(2, "Second"))
        timestamp = 300L
        repository.recordViewed(movie(1, "First updated"))

        val history = repository.observeHistory().first()
        assertEquals(2, history.size)
        assertEquals(listOf(1, 2), history.map { it.tmdbId })
        assertEquals("First updated", history.first().title)
        assertEquals(300L, history.first().viewedAt)
    }

    @Test
    fun `recordSentToTv marks entry as sent to TV`() = runTest {
        var timestamp = 100L
        val repository = RoomHistoryRepository(database.historyDao()) { timestamp }

        repository.recordViewed(movie(1, "First"))
        assertFalse(repository.observeHistory().first().first().sentToTv)

        timestamp = 200L
        repository.recordSentToTv(movie(1, "First"))

        val history = repository.observeHistory().first()
        assertEquals(1, history.size)
        assertTrue(history.first().sentToTv)
        assertEquals(200L, history.first().viewedAt)
    }

    private fun movie(id: Int, title: String) = MovieDetail(
        id = id,
        title = title,
        posterUrl = null,
        overview = "",
        releaseDate = "",
        voteAverage = 0.0,
        genres = emptyList(),
    )
}
