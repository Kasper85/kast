package com.kastlg.app.presentation.tvdetail

import com.kastlg.app.domain.models.Episode
import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.Season

data class TvShowDetailUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String = "",
    val releaseYear: String = "",
    val voteAverage: Double = 0.0,
    val genres: List<Genre> = emptyList(),
    val numberOfSeasons: Int = 0,
    val numberOfEpisodes: Int = 0,
    val status: String = "",
    val errorMessage: String? = null,
    val tvErrorMessage: String? = null,
    val tvSuccessMessage: String? = null,
    val seasons: List<Season> = emptyList(),
    val selectedSeason: Season? = null,
    val isLoadingSeason: Boolean = false,
    val selectedEpisode: Episode? = null,
) {
    val tvMessage: String? get() = tvErrorMessage ?: tvSuccessMessage
}
