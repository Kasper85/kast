package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FlixcornSeriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(series: FlixcornSeriesEntity)

    @Query("SELECT * FROM flixcorn_series WHERE slug = :slug AND expires_at > :now LIMIT 1")
    suspend fun getBySlug(slug: String, now: Long = System.currentTimeMillis()): FlixcornSeriesEntity?

    @Query("DELETE FROM flixcorn_series WHERE expires_at <= :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM flixcorn_series")
    suspend fun deleteAll()
}
