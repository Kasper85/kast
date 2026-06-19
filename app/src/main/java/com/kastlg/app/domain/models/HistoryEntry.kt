package com.kastlg.app.domain.models

data class HistoryEntry(
    val tmdbId: Int,
    val title: String,
    val posterUrl: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val viewedAt: Long,
    val sentToTv: Boolean = false,
)
