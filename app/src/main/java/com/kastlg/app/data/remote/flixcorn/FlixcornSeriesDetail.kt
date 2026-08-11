package com.kastlg.app.data.remote.flixcorn

data class FlixcornSeriesDetail(
    val slug: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String,
    val year: Int?,
    val rating: Double?,
    val genres: List<String>,
    val numberOfSeasons: Int,
    val numberOfEpisodes: Int,
    val status: String,
    val seasons: List<FlixcornSeason>,
)

data class FlixcornSeason(
    val seasonNumber: Int,
    val episodes: List<FlixcornEpisode>,
)

data class FlixcornEpisode(
    val episodeNumber: Int,
    val title: String,
    val synopsis: String,
    val seasonNumber: Int,
)
