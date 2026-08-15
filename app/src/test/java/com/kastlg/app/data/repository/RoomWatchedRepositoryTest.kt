package com.kastlg.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kastlg.app.data.local.WatchedEpisodeDao
import com.kastlg.app.data.local.WatchedEpisodeEntity
import com.kastlg.app.data.local.WatchedMovieDao
import com.kastlg.app.data.local.WatchedMovieEntity
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
class RoomWatchedRepositoryTest {
    private lateinit var database: com.kastlg.app.data.local.KastLgDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            com.kastlg.app.data.local.KastLgDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `real Room movie toggle inserts removes and stores watchedAt`() = runTest {
        var timestamp = 100L
        val repository = RoomWatchedRepository(
            database.watchedMovieDao(),
            database.watchedEpisodeDao(),
        ) { timestamp }

        repository.toggleMovie(1)
        timestamp = 200L
        repository.toggleMovie(2)

        assertTrue(repository.observeIsMovieWatched(1).first())
        assertTrue(repository.observeIsMovieWatched(2).first())
        assertEquals(
            setOf(1L to 100L, 2L to 200L),
            watchedMovies().toSet(),
        )

        timestamp = 300L
        repository.toggleMovie(1)
        assertFalse(repository.observeIsMovieWatched(1).first())
        assertEquals(listOf(2L to 200L), watchedMovies())
    }

    @Test
    fun `real Room episode toggle isolates composite key rows`() = runTest {
        var timestamp = 100L
        val repository = RoomWatchedRepository(
            database.watchedMovieDao(),
            database.watchedEpisodeDao(),
        ) { timestamp }

        repository.toggleEpisode("breaking-bad", 1, 1)
        timestamp = 200L
        repository.toggleEpisode("breaking-bad", 1, 2)
        timestamp = 300L
        repository.toggleEpisode("breaking-bad", 2, 1)

        assertTrue(repository.observeIsEpisodeWatched("breaking-bad", 1, 1).first())
        assertTrue(repository.observeIsEpisodeWatched("breaking-bad", 1, 2).first())
        assertTrue(repository.observeIsEpisodeWatched("breaking-bad", 2, 1).first())

        // Composite key isolation: same slug, different season/episode stay untouched.
        repository.toggleEpisode("breaking-bad", 1, 1)
        assertFalse(repository.observeIsEpisodeWatched("breaking-bad", 1, 1).first())
        assertTrue(repository.observeIsEpisodeWatched("breaking-bad", 1, 2).first())
        assertTrue(repository.observeIsEpisodeWatched("breaking-bad", 2, 1).first())
        assertEquals(
            setOf(
                Triple("breaking-bad", 1 to 2, 200L),
                Triple("breaking-bad", 2 to 1, 300L),
            ),
            watchedEpisodes().toSet(),
        )
    }

    @Test
    fun `concurrent real Room toggles preserve parity`() = runTest {
        val repository = RoomWatchedRepository(
            database.watchedMovieDao(),
            database.watchedEpisodeDao(),
        ) { 100L }

        List(20) {
            async { repository.toggleMovie(550) }
        }.awaitAll()

        assertFalse(repository.observeIsMovieWatched(550).first())
        assertTrue(watchedMovies().isEmpty())
    }

    @Test
    fun `repository serializes concurrent movie toggles before DAO access`() = runTest {
        val dao = SlowToggleMovieDao()
        val repository = RoomWatchedRepository(dao, database.watchedEpisodeDao()) { 100L }

        List(10) {
            async { repository.toggleMovie(550) }
        }.awaitAll()

        assertEquals(1, dao.maxConcurrentCalls.get())
        assertFalse(dao.isWatched)
    }

    @Test
    fun `repository serializes concurrent episode toggles before DAO access`() = runTest {
        val dao = SlowToggleEpisodeDao()
        val repository = RoomWatchedRepository(database.watchedMovieDao(), dao) { 100L }

        List(10) {
            async { repository.toggleEpisode("breaking-bad", 1, 1) }
        }.awaitAll()

        assertEquals(1, dao.maxConcurrentCalls.get())
        assertFalse(dao.isWatched)
    }

    private class SlowToggleMovieDao : WatchedMovieDao() {
        val maxConcurrentCalls = AtomicInteger()
        private val activeCalls = AtomicInteger()
        var isWatched = false

        override fun observeExists(movieId: Int): Flow<Boolean> = emptyFlow()

        override suspend fun exists(movieId: Int): Boolean = isWatched

        override suspend fun upsert(entity: WatchedMovieEntity) {
            isWatched = true
        }

        override suspend fun delete(movieId: Int) {
            isWatched = false
        }

        override suspend fun toggle(entity: WatchedMovieEntity) {
            val active = activeCalls.incrementAndGet()
            maxConcurrentCalls.updateAndGet { current -> maxOf(current, active) }
            delay(10)
            isWatched = !isWatched
            activeCalls.decrementAndGet()
        }
    }

    private class SlowToggleEpisodeDao : WatchedEpisodeDao() {
        val maxConcurrentCalls = AtomicInteger()
        private val activeCalls = AtomicInteger()
        var isWatched = false

        override fun observeExists(slug: String, season: Int, episode: Int): Flow<Boolean> = emptyFlow()

        override suspend fun exists(slug: String, season: Int, episode: Int): Boolean = isWatched

        override suspend fun upsert(entity: WatchedEpisodeEntity) {
            isWatched = true
        }

        override suspend fun delete(slug: String, season: Int, episode: Int) {
            isWatched = false
        }

        override suspend fun toggle(entity: WatchedEpisodeEntity) {
            val active = activeCalls.incrementAndGet()
            maxConcurrentCalls.updateAndGet { current -> maxOf(current, active) }
            delay(10)
            isWatched = !isWatched
            activeCalls.decrementAndGet()
        }
    }

    private fun watchedMovies(): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()
        database.openHelper.readableDatabase.query("SELECT movieId, watchedAt FROM watched_movies").use { cursor ->
            while (cursor.moveToNext()) {
                result += cursor.getLong(0) to cursor.getLong(1)
            }
        }
        return result.sortedBy { it.first }
    }

    private fun watchedEpisodes(): List<Triple<String, Pair<Int, Int>, Long>> {
        val result = mutableListOf<Triple<String, Pair<Int, Int>, Long>>()
        database.openHelper.readableDatabase.query("SELECT slug, season, episode, watchedAt FROM watched_episodes").use { cursor ->
            while (cursor.moveToNext()) {
                result += Triple(cursor.getString(0), cursor.getInt(1) to cursor.getInt(2), cursor.getLong(3))
            }
        }
        return result.sortedBy { it.first }
    }
}