package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FlixcornSeriesFavoriteDao {
    @Query("SELECT * FROM flixcorn_series_favorites WHERE slug = :slug")
    abstract fun observeBySlug(slug: String): Flow<FlixcornSeriesFavoriteEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM flixcorn_series_favorites WHERE slug = :slug)")
    abstract fun existsBySlug(slug: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(favorite: FlixcornSeriesFavoriteEntity)

    @Query("DELETE FROM flixcorn_series_favorites WHERE slug = :slug")
    abstract suspend fun deleteBySlug(slug: String)
}