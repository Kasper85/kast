package com.kastlg.app.domain.models

data class Episode(
    val id: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String,
    val airDate: String,
    val stillUrl: String?,
    val seasonNumber: Int,
)
