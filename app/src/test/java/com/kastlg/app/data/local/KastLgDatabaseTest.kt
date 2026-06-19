package com.kastlg.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kastlg.app.data.repository.RoomFavoriteRepository
import com.kastlg.app.data.repository.RoomHistoryRepository
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
class KastLgDatabaseTest {
    private lateinit var database: KastLgDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            KastLgDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `favorite DAO toggles persistence and orders newest first`() = runTest {
        val dao = database.favoriteDao()
        assertFalse(dao.exists(1))

        dao.upsert(favorite(1, "First", 100L))
        dao.upsert(favorite(2, "Second", 200L))

        assertTrue(dao.exists(1))
        assertEquals(listOf(2, 1), dao.observeAll().first().map { it.tmdbId })

        dao.deleteById(1)
        assertFalse(dao.exists(1))
        assertEquals(listOf(2), dao.observeAll().first().map { it.tmdbId })
    }

    @Test
    fun `history DAO deduplicates movie and moves revisit to newest`() = runTest {
        val dao = database.historyDao()
        dao.upsert(history(1, "First", 100L))
        dao.upsert(history(2, "Second", 200L))
        dao.upsert(history(1, "First updated", 300L))

        val entries = dao.observeAll().first()

        assertEquals(2, entries.size)
        assertEquals(listOf(1, 2), entries.map { it.tmdbId })
        assertEquals("First updated", entries.first().title)
        assertEquals(300L, entries.first().viewedAt)
    }

    @Test
    fun `version three database preserves favorite and history repositories after reopen`() = runTest {
        database.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "phase4-persistence.db"
        context.deleteDatabase(databaseName)

        database = Room.databaseBuilder(context, KastLgDatabase::class.java, databaseName)
            .addMigrations(*DatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        RoomFavoriteRepository(database.favoriteDao()) { 100L }
            .toggle(movie(550, "Fight Club"))
        RoomHistoryRepository(database.historyDao()) { 200L }
            .recordViewed(movie(550, "Fight Club"))
        assertEquals(3, database.openHelper.readableDatabase.version)
        database.close()

        database = Room.databaseBuilder(context, KastLgDatabase::class.java, databaseName)
            .addMigrations(*DatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        assertEquals(
            listOf(550),
            RoomFavoriteRepository(database.favoriteDao()).observeFavorites().first().map { it.tmdbId },
        )
        assertEquals(
            listOf(550),
            RoomHistoryRepository(database.historyDao()).observeHistory().first().map { it.tmdbId },
        )
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `schema export and migration registry match configuration`() {
        val schemaV1 = java.io.File(
            "schemas/com.kastlg.app.data.local.KastLgDatabase/1.json",
        )
        assertTrue("Room schema v1 must be exported", schemaV1.isFile)
        val contentsV1 = schemaV1.readText()
        assertTrue(contentsV1.contains("\"version\": 1"))
        assertTrue(contentsV1.contains("\"tableName\": \"favorites\""))
        assertTrue(contentsV1.contains("\"tableName\": \"history\""))

        val schemaV2 = java.io.File(
            "schemas/com.kastlg.app.data.local.KastLgDatabase/2.json",
        )
        assertTrue("Room schema v2 must be exported", schemaV2.isFile)
        val contentsV2 = schemaV2.readText()
        assertTrue(contentsV2.contains("\"version\": 2"))
        assertTrue(contentsV2.contains("\"tableName\": \"tv_config\""))

        val schemaV3 = java.io.File(
            "schemas/com.kastlg.app.data.local.KastLgDatabase/3.json",
        )
        assertTrue("Room schema v3 must be exported", schemaV3.isFile)
        val contentsV3 = schemaV3.readText()
        assertTrue(contentsV3.contains("\"version\": 3"))
        assertTrue(contentsV3.contains("\"sent_to_tv\""))

        assertEquals(2, DatabaseMigrations.ALL.size)
    }

    private fun favorite(id: Int, title: String, timestamp: Long) = FavoriteEntity(
        tmdbId = id,
        title = title,
        posterUrl = null,
        overview = "",
        releaseDate = "",
        voteAverage = 0.0,
        favoritedAt = timestamp,
    )

    private fun history(id: Int, title: String, timestamp: Long) = HistoryEntity(
        tmdbId = id,
        title = title,
        posterUrl = null,
        overview = "",
        releaseDate = "",
        voteAverage = 0.0,
        viewedAt = timestamp,
    )

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
