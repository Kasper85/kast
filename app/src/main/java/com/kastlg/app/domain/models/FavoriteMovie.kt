package com.kastlg.app.domain.models

data class FavoriteMovie(
    val tmdbId: Int,
    val title: String,
    val posterUrl: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val favoritedAt: Long,
)
