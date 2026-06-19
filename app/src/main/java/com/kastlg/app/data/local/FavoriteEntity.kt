package com.kastlg.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int,
    val title: String,
    @ColumnInfo(name = "poster_url")
    val posterUrl: String?,
    val overview: String,
    @ColumnInfo(name = "release_date")
    val releaseDate: String,
    @ColumnInfo(name = "vote_average")
    val voteAverage: Double,
    @ColumnInfo(name = "favorited_at")
    val favoritedAt: Long,
)
