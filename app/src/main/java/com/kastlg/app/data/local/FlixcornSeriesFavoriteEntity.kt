package com.kastlg.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flixcorn_series_favorites",
    indices = [Index(value = ["slug"])],
)
data class FlixcornSeriesFavoriteEntity(
    @PrimaryKey
    val slug: String,
    val title: String,
    @ColumnInfo(name = "poster_url")
    val posterUrl: String?,
    val favoritedAt: Long,
)