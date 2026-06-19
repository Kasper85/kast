package com.kastlg.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
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
    @ColumnInfo(name = "viewed_at")
    val viewedAt: Long,
    @ColumnInfo(name = "sent_to_tv", defaultValue = "0")
    val sentToTv: Boolean = false,
)
