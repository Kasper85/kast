package com.kastlg.app.domain.models

data class Season(
    val id: Int,
    val seasonNumber: Int,
    val name: String,
    val posterUrl: String?,
    val episodeCount: Int,
    val episodes: List<Episode>,
)
