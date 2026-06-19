package com.kastlg.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.kastlg.app.domain.models.MovieDetail

data class MovieDetailDto(
    val id: Int,
    val title: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    val overview: String,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double,
    val genres: List<GenreDto>,
)

fun MovieDetailDto.toDomain(): MovieDetail = MovieDetail(
    id = id,
    title = title,
    posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
    overview = overview,
    releaseDate = releaseDate.orEmpty(),
    voteAverage = voteAverage,
    genres = genres.map { it.toDomain() },
)
