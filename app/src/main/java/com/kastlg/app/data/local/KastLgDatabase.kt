package com.kastlg.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        TvConfigEntity::class,
        FlixcornSeriesEntity::class,
        FlixcornEpisodeCacheEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class KastLgDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun historyDao(): HistoryDao

    abstract fun tvConfigDao(): TvConfigDao

    abstract fun flixcornSeriesDao(): FlixcornSeriesDao

    abstract fun flixcornEpisodeCacheDao(): FlixcornEpisodeCacheDao

    companion object {
        private const val DATABASE_NAME = "kastlg.db"

        fun create(context: Context): KastLgDatabase = Room.databaseBuilder(
            context.applicationContext,
            KastLgDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
    }
}
