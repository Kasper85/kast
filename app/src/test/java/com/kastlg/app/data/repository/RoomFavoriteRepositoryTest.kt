package com.kastlg.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kastlg.app.data.local.FavoriteDao
import com.kastlg.app.data.local.FavoriteEntity
import com.kastlg.app.data.local.KastLgDatabase
import com.kastlg.app.domain.models.MovieDetail
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
class RoomFavoriteRepositoryTest {
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
    fun `real Room toggle inserts removes and orders favorites`() = runTest {
        var timestamp = 100L
        val repository = RoomFavoriteRepository(database.favoriteDao()) { timestamp }

        repository.toggle(movie(1, "First"))
        timestamp = 200L
        repository.toggle(movie(2, "Second"))

        assertEquals(listOf(2, 1), repository.observeFavorites().first().map { it.tmdbId })

        repository.toggle(movie(1, "First"))
        assertFalse(repository.observeIsFavorite(1).first())

        timestamp = 300L
        repository.toggle(movie(1, "First again"))
        val favorites = repository.observeFavorites().first()
        assertEquals(listOf(1, 2), favorites.map { it.tmdbId })
        assertEquals("First again", favorites.first().title)
    }

    @Test
    fun `concurrent real Room toggles preserve parity`() = runTest {
        val repository = RoomFavoriteRepository(database.favoriteDao()) { 100L }

        List(20) {
            async { repository.toggle(movie(550, "Fight Club")) }
        }.awaitAll()

        assertFalse(repository.observeIsFavorite(550).first())
        assertTrue(repository.observeFavorites().first().isEmpty())
    }

    @Test
    fun `repository serializes concurrent toggle calls before DAO access`() = runTest {
        val dao = SlowToggleDao()
        val repository = RoomFavoriteRepository(dao) { 100L }

        List(10) {
            async { repository.toggle(movie(550, "Fight Club")) }
        }.awaitAll()

        assertEquals(1, dao.maxConcurrentCalls.get())
        assertFalse(dao.isFavorite)
    }

    private class SlowToggleDao : FavoriteDao() {
        val maxConcurrentCalls = AtomicInteger()
        private val activeCalls = AtomicInteger()
        var isFavorite = false

        override fun observeAll(): Flow<List<FavoriteEntity>> = emptyFlow()

        override fun observeExists(tmdbId: Int): Flow<Boolean> = emptyFlow()

        override suspend fun exists(tmdbId: Int): Boolean = isFavorite

        override suspend fun upsert(favorite: FavoriteEntity) {
            isFavorite = true
        }

        override suspend fun deleteById(tmdbId: Int) {
            isFavorite = false
        }

        override suspend fun toggle(favorite: FavoriteEntity) {
            val active = activeCalls.incrementAndGet()
            maxConcurrentCalls.updateAndGet { current -> maxOf(current, active) }
            delay(10)
            isFavorite = !isFavorite
            activeCalls.decrementAndGet()
        }
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
