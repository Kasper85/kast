package com.kastlg.app.domain.models

data class Movie(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val genreIds: List<Int>,
)
