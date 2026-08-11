package com.kastlg.app.data.remote.flixcorn

data class FlixcornSearchResult(
    val title: String,
    val slug: String,
    val year: Int?,
    val posterUrl: String?,
    val genres: List<String>,
)
