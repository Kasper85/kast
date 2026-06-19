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
     * Version 1 is the initial schema. Future schema changes must add an explicit
     * Migration here; destructive fallback is intentionally not enabled.
     */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
