package com.kastlg.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SeasonDto(
    val id: Int,
    @SerializedName("season_number")
    val seasonNumber: Int,
    val name: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("episode_count")
    val episodeCount: Int,
    val episodes: List<EpisodeDto>,
)

data class EpisodeDto(
    val id: Int,
    @SerializedName("episode_number")
    val episodeNumber: Int,
    val name: String,
    val overview: String,
    @SerializedName("air_date")
    val airDate: String?,
    @SerializedName("still_path")
    val stillPath: String?,
    @SerializedName("season_number")
    val seasonNumber: Int,
)

fun SeasonDto.toDomain() = com.kastlg.app.domain.models.Season(
    id = id,
    seasonNumber = seasonNumber,
    name = name,
    posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
    episodeCount = episodeCount,
    episodes = episodes.map { it.toDomain() },
)

fun EpisodeDto.toDomain() = com.kastlg.app.domain.models.Episode(
    id = id,
    episodeNumber = episodeNumber,
    name = name,
    overview = overview,
    airDate = airDate.orEmpty(),
    stillUrl = stillPath?.let { "https://image.tmdb.org/t/p/w300$it" },
    seasonNumber = seasonNumber,
)
