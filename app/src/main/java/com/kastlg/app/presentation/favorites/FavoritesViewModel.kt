package com.kastlg.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.domain.repositories.FavoriteRepository
import com.kastlg.app.presentation.library.SavedMovieItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val movies: List<SavedMovieItem> = emptyList(),
    val errorMessage: String? = null,
)

class FavoritesViewModel(
    repository: FavoriteRepository,
) : ViewModel() {
    val uiState: StateFlow<FavoritesUiState> = repository.observeFavorites()
        .map { favorites ->
            FavoritesUiState(
                isLoading = false,
                movies = favorites.map {
                    SavedMovieItem(
                        tmdbId = it.tmdbId,
                        title = it.title,
                        posterUrl = it.posterUrl,
                        releaseDate = it.releaseDate,
                        voteAverage = it.voteAverage,
                    )
                },
            )
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(
                FavoritesUiState(
                    isLoading = false,
                    errorMessage = "No se pudieron cargar los favoritos.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FavoritesUiState(),
        )
}
