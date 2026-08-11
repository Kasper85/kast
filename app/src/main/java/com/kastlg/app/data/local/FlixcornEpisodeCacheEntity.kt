package com.kastlg.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flixcorn_episode_cache")
data class FlixcornEpisodeCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "episode_url")
    val episodeUrl: String,
    @ColumnInfo(name = "servers_json")
    val serversJson: String,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,
)
