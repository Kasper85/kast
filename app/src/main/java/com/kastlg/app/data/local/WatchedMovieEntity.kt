package com.kastlg.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_movies")
data class WatchedMovieEntity(
    @PrimaryKey
    val movieId: Int,
    val watchedAt: Long,
)