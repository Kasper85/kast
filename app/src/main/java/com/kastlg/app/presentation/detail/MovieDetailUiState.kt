package com.kastlg.app.presentation.detail

import com.kastlg.app.domain.models.Genre

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val posterUrl: String? = null,
    val overview: String = "",
    val releaseYear: String = "",
    val voteAverage: Double = 0.0,
    val genres: List<Genre> = emptyList(),
    val isFavorite: Boolean = false,
    val errorMessage: String? = null,
    val favoriteErrorMessage: String? = null,
    val historyErrorMessage: String? = null,
    val tvErrorMessage: String? = null,
    val tvSuccessMessage: String? = null,
) {
    val persistenceErrorMessage: String?
        get() = favoriteErrorMessage ?: historyErrorMessage

    val tvMessage: String?
        get() = tvErrorMessage ?: tvSuccessMessage
}
