package com.kastlg.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tv_config` (
                    `id` INTEGER NOT NULL DEFAULT 1,
                    `tv_ip` TEXT NOT NULL,
                    `tv_name` TEXT NOT NULL,
                    `client_key` TEXT,
                    `is_paired` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `history` ADD COLUMN `sent_to_tv` INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    /**
     * Adds target_type, bridge_url, and device_port columns to tv_config
     * to support Apple TV targets alongside LG webOS.
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `tv_config` ADD COLUMN `target_type` TEXT NOT NULL DEFAULT 'LG_WEBOS'",
            )
            db.execSQL(
                "ALTER TABLE `tv_config` ADD COLUMN `bridge_url` TEXT",
            )
            db.execSQL(
                "ALTER TABLE `tv_config` ADD COLUMN `device_port` INTEGER NOT NULL DEFAULT 7000",
            )
        }
    }

    /**
     * Version 1 is the initial schema. Future schema changes must add an explicit
     * Migration here; destructive fallback is intentionally not enabled.
     */

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flixcorn_series` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `slug` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `poster_url` TEXT,
                    `backdrop_url` TEXT,
                    `overview` TEXT NOT NULL,
                    `year` INTEGER,
                    `rating` REAL,
                    `genres` TEXT NOT NULL,
                    `number_of_seasons` INTEGER NOT NULL,
                    `number_of_episodes` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `detail_url` TEXT NOT NULL,
                    `cached_at` INTEGER NOT NULL,
                    `expires_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_flixcorn_series_slug` ON `flixcorn_series` (`slug`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flixcorn_episode_cache` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `episode_url` TEXT NOT NULL,
                    `servers_json` TEXT NOT NULL,
                    `cached_at` INTEGER NOT NULL,
                    `expires_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_flixcorn_episode_cache_episode_url` ON `flixcorn_episode_cache` (`episode_url`)")
        }
    }

    /**
     * Migration 5_6: Adds favorite tracking for flixcorn series.
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flixcorn_series_favorites` (
                    `slug` TEXT NOT NULL PRIMARY KEY,
                    `title` TEXT NOT NULL,
                    `poster_url` TEXT,
                    `favorited_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_flixcorn_series_favorites_slug` ON `flixcorn_series_favorites` (`slug`)",
            )
        }
    }

    /**
     * Migration 6_7 (DLS-2 / DLS-3 / DLS-4):
     *  - Declares the missing unique indices for flixcorn tables. No-op on the
     *    v4->v5 path (already created there); REQUIRED for fresh-v6 installs,
     *    whose exported schema declared no indices.
     *  - Rebuilds `favorites` to drop the legacy `media_type` column, preserving
     *    every row.
     *  - Rebuilds `flixcorn_series_favorites` onto the entity shape (column
     *    `favoritedAt`). The on-disk column differs by origin: MIGRATION_5_6
     *    created `favorited_at` (snake_case) while fresh-v6 uses `favoritedAt`
     *    (camelCase), so the source column is detected at runtime via PRAGMA.
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_flixcorn_series_slug` ON `flixcorn_series` (`slug`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_flixcorn_episode_cache_episode_url` ON `flixcorn_episode_cache` (`episode_url`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favorites_new` (
                    `tmdb_id` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `poster_url` TEXT,
                    `overview` TEXT NOT NULL,
                    `release_date` TEXT NOT NULL,
                    `vote_average` REAL NOT NULL,
                    `favorited_at` INTEGER NOT NULL,
                    PRIMARY KEY(`tmdb_id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `favorites_new` (`tmdb_id`, `title`, `poster_url`, `overview`, `release_date`, `vote_average`, `favorited_at`)
                SELECT `tmdb_id`, `title`, `poster_url`, `overview`, `release_date`, `vote_average`, `favorited_at` FROM `favorites`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `favorites`")
            db.execSQL("ALTER TABLE `favorites_new` RENAME TO `favorites`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flixcorn_series_favorites_new` (
                    `slug` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `poster_url` TEXT,
                    `favoritedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`slug`)
                )
                """.trimIndent(),
            )
            val timestampColumn = flixcornSeriesFavoritesTimestampColumn(db)
            db.execSQL(
                """
                INSERT INTO `flixcorn_series_favorites_new` (`slug`, `title`, `poster_url`, `favoritedAt`)
                SELECT `slug`, `title`, `poster_url`, `$timestampColumn` FROM `flixcorn_series_favorites`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `flixcorn_series_favorites`")
            db.execSQL("ALTER TABLE `flixcorn_series_favorites_new` RENAME TO `flixcorn_series_favorites`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_flixcorn_series_favorites_slug` ON `flixcorn_series_favorites` (`slug`)",
            )
        }
    }

    /**
     * Migration 7_8: Adds watched-tracking tables for movies and episodes.
     * Column names and order match the entity-generated schema exactly
     * (camelCase, no DEFAULT clauses) so Room's TableInfo comparison passes.
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `watched_movies` (
                    `movieId` INTEGER NOT NULL,
                    `watchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`movieId`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `watched_episodes` (
                    `slug` TEXT NOT NULL,
                    `season` INTEGER NOT NULL,
                    `episode` INTEGER NOT NULL,
                    `watchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`slug`, `season`, `episode`)
                )
                """.trimIndent(),
            )
        }
    }

    /**
     * Returns the actual timestamp column name of `flixcorn_series_favorites`
     * on this device: `favorited_at` when the table came from MIGRATION_5_6,
     * `favoritedAt` when it was created by a fresh v6 install. The name is
     * returned bare (no backticks); callers wrap it in their own SQL.
     */
    private fun flixcornSeriesFavoritesTimestampColumn(db: SupportSQLiteDatabase): String {
        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(`flixcorn_series_favorites`)").use { cursor ->
            while (cursor.moveToNext()) {
                columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
        }
        return when {
            "favorited_at" in columns -> "favorited_at"
            "favoritedAt" in columns -> "favoritedAt"
            else -> error("flixcorn_series_favorites has no timestamp column")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
    )
}
