package com.kastlg.app.data.local

import androidx.room.Entity

@Entity(
    tableName = "watched_episodes",
    primaryKeys = ["slug", "season", "episode"],
)
data class WatchedEpisodeEntity(
    val slug: String,
    val season: Int,
    val episode: Int,
    val watchedAt: Long,
)