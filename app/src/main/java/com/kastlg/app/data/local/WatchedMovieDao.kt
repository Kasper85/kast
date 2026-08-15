package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WatchedMovieDao {
    @Query("SELECT EXISTS(SELECT 1 FROM watched_movies WHERE movieId = :movieId)")
    abstract fun observeExists(movieId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watched_movies WHERE movieId = :movieId)")
    abstract suspend fun exists(movieId: Int): Boolean

    @Upsert
    abstract suspend fun upsert(entity: WatchedMovieEntity)

    @Query("DELETE FROM watched_movies WHERE movieId = :movieId")
    abstract suspend fun delete(movieId: Int)

    @Transaction
    open suspend fun toggle(entity: WatchedMovieEntity) {
        if (exists(entity.movieId)) {
            delete(entity.movieId)
        } else {
            upsert(entity)
        }
    }
}