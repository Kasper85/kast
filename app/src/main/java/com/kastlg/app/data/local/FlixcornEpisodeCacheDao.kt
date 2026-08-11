package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FlixcornEpisodeCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: FlixcornEpisodeCacheEntity)

    @Query("SELECT * FROM flixcorn_episode_cache WHERE episode_url = :episodeUrl AND expires_at > :now LIMIT 1")
    suspend fun getByUrl(
        episodeUrl: String,
        now: Long = System.currentTimeMillis(),
    ): FlixcornEpisodeCacheEntity?

    @Query("DELETE FROM flixcorn_episode_cache WHERE expires_at <= :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM flixcorn_episode_cache")
    suspend fun deleteAll()
}
