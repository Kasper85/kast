package com.kastlg.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.kastlg.app.domain.models.TvShow

data class TvShowDto(
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
    @SerializedName("genre_ids")
    val genreIds: List<Int>,
    @SerializedName("origin_country")
    val originCountry: List<String>,
)

fun TvShowDto.toDomain(): TvShow = TvShow(
    id = id,
    title = name,
    posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
    backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
    overview = overview,
    releaseDate = firstAirDate.orEmpty(),
    voteAverage = voteAverage,
    genreIds = genreIds,
    originCountry = originCountry,
)
