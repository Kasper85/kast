package com.kastlg.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flixcorn_series",
    indices = [Index(value = ["slug"], unique = true)],
)
data class FlixcornSeriesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val slug: String,
    val title: String,
    @ColumnInfo(name = "poster_url")
    val posterUrl: String?,
    @ColumnInfo(name = "backdrop_url")
    val backdropUrl: String?,
    val overview: String,
    val year: Int?,
    val rating: Double?,
    val genres: String,
    @ColumnInfo(name = "number_of_seasons")
    val numberOfSeasons: Int,
    @ColumnInfo(name = "number_of_episodes")
    val numberOfEpisodes: Int,
    val status: String,
    @ColumnInfo(name = "detail_url")
    val detailUrl: String,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,
)
