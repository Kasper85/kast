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

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
