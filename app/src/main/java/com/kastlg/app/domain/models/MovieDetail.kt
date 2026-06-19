package com.kastlg.app.domain.models

data class MovieDetail(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val genres: List<Genre>,
)
