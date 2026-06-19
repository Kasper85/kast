package com.kastlg.app.data.tv

object PlaybackUrlBuilder {
    private const val BASE_URL = "https://unlimplay.com/play/embed"

    fun buildMovieUrl(movieId: Int): String = "$BASE_URL/movie/$movieId"

    fun buildTvUrl(tvShowId: Int, seasonNumber: Int? = null, episodeNumber: Int? = null): String {
        val base = "$BASE_URL/tv/$tvShowId"
        return if (seasonNumber != null && episodeNumber != null) {
            "$base?season=$seasonNumber&episode=$episodeNumber"
        } else {
            base
        }
    }
}
