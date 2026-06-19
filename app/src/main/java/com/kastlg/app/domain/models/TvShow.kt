package com.kastlg.app.domain.models

data class TvShow(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val genreIds: List<Int>,
    val originCountry: List<String>,
)
