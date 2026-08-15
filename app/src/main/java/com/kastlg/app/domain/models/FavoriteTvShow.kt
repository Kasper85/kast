package com.kastlg.app.domain.models

data class FavoriteTvShow(
    val tmdbId: Int,
    val title: String,
    val posterUrl: String?,
    val overview: String,
    val firstAirDate: String,
    val voteAverage: Double,
    val favoritedAt: Long,
)