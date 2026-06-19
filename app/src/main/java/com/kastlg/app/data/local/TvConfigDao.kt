package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TvConfigDao {
    @Query("SELECT * FROM tv_config LIMIT 1")
    abstract fun observe(): Flow<TvConfigEntity?>

    @Query("SELECT * FROM tv_config LIMIT 1")
    abstract suspend fun get(): TvConfigEntity?

    @Upsert
    abstract suspend fun upsert(config: TvConfigEntity)

    @Query("DELETE FROM tv_config")
    abstract suspend fun deleteAll()
}
