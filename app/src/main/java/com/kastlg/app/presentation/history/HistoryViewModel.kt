package com.kastlg.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.domain.repositories.HistoryRepository
import com.kastlg.app.presentation.library.SavedMovieItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val isLoading: Boolean = true,
    val movies: List<SavedMovieItem> = emptyList(),
    val errorMessage: String? = null,
)

class HistoryViewModel(
    repository: HistoryRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = repository.observeHistory()
        .map { history ->
            HistoryUiState(
                isLoading = false,
                movies = history.map {
                    SavedMovieItem(
                        tmdbId = it.tmdbId,
                        title = it.title,
                        posterUrl = it.posterUrl,
                        releaseDate = it.releaseDate,
                        voteAverage = it.voteAverage,
                        sentToTv = it.sentToTv,
                    )
                },
            )
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(
                HistoryUiState(
                    isLoading = false,
                    errorMessage = "No se pudo cargar el historial.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HistoryUiState(),
        )
}
