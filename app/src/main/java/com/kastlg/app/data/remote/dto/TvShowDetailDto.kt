package com.kastlg.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.kastlg.app.domain.models.TvShowDetail

data class TvShowDetailDto(
    val id: Int,
    val name: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("backdrop_path")
    val backdropPath: String?,
    val overview: String,
    @SerializedName("first_air_date")
    val firstAirDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double,
    val genres: List<GenreDto>,
    @SerializedName("number_of_seasons")
    val numberOfSeasons: Int,
    @SerializedName("number_of_episodes")
    val numberOfEpisodes: Int,
    @SerializedName("status")
    val status: String,
)

fun TvShowDetailDto.toDomain(): TvShowDetail = TvShowDetail(
    id = id,
    title = name,
    posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
    backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
    overview = overview,
    releaseDate = firstAirDate.orEmpty(),
    voteAverage = voteAverage,
    genres = genres.map { it.toDomain() },
    numberOfSeasons = numberOfSeasons,
    numberOfEpisodes = numberOfEpisodes,
    status = status,
)
