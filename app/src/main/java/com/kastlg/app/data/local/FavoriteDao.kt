package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY favorited_at DESC, tmdb_id DESC")
    abstract fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE tmdb_id = :tmdbId)")
    abstract fun observeExists(tmdbId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE tmdb_id = :tmdbId)")
    abstract suspend fun exists(tmdbId: Int): Boolean

    @Upsert
    abstract suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE tmdb_id = :tmdbId")
    abstract suspend fun deleteById(tmdbId: Int)

    @Transaction
    open suspend fun toggle(favorite: FavoriteEntity) {
        if (exists(favorite.tmdbId)) {
            deleteById(favorite.tmdbId)
        } else {
            upsert(favorite)
        }
    }
}
