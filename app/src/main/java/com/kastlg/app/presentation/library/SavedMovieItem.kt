package com.kastlg.app.presentation.library

data class SavedMovieItem(
    val tmdbId: Int,
    val title: String,
    val posterUrl: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val sentToTv: Boolean = false,
)
