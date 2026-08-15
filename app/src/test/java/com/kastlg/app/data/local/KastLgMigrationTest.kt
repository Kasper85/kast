package com.kastlg.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KastLgMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KastLgDatabase::class.java,
    )

    @Test
    fun `migration 7 to 8 preserves favorites and creates empty watched tables`() {
        // Seed a v7 database (from the exported 7.json schema) with one favorite row.
        helper.createDatabase(TEST_DB, 7).use { db ->
            db.execSQL(
                """
                INSERT INTO favorites (tmdb_id, title, poster_url, overview, release_date, vote_average, favorited_at)
                VALUES (550, 'Fight Club', NULL, 'An insomniac office worker and a soap maker form an underground fight club.', '1999-10-15', 8.4, 100)
                """.trimIndent(),
            )
        }

        // Run every registered migration (7->8 included) and validate against 8.json.
        val migrated = helper.runMigrationsAndValidate(TEST_DB, 8, true, *DatabaseMigrations.ALL)
        migrated.use { db ->
            // Favorites row is preserved with original values.
            db.query("SELECT tmdb_id, title, favorited_at FROM favorites").use { cursor ->
                assertTrue("favorite row must survive the migration", cursor.moveToFirst())
                assertEquals(550, cursor.getInt(cursor.getColumnIndexOrThrow("tmdb_id")))
                assertEquals("Fight Club", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                assertEquals(100L, cursor.getLong(cursor.getColumnIndexOrThrow("favorited_at")))
                assertFalse("only the seeded favorite row may exist", cursor.moveToNext())
            }

            // Both watched tables are created empty by the migration.
            db.query("SELECT COUNT(*) FROM watched_movies").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM watched_episodes").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            // ... and are writable with the exact columns Room declares for v8.
            db.execSQL("INSERT INTO watched_movies (movieId, watchedAt) VALUES (1, 100)")
            db.execSQL(
                "INSERT INTO watched_episodes (slug, season, episode, watchedAt) VALUES ('breaking-bad', 1, 1, 200)",
            )
            db.query("SELECT COUNT(*) FROM watched_movies").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "kastlg-migration-test.db"
    }
}
