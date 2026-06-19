package com.kastlg.app.domain.models

data class TvShowDetail(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String,
    val releaseDate: String,
    val voteAverage: Double,
    val genres: List<Genre>,
    val numberOfSeasons: Int,
    val numberOfEpisodes: Int,
    val status: String,
)
