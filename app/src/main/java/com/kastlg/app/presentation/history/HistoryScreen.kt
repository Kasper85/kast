package com.kastlg.app.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kastlg.app.di.AppContainer
import com.kastlg.app.presentation.library.SavedMoviesScreen

@Composable
fun HistoryRoute(
    onMovieClick: (Int) -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(AppContainer.historyRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SavedMoviesScreen(
        title = "Historial",
        emptyMessage = "No has visto contenido todavía",
        emptyCtaLabel = "Ir al inicio",
        onEmptyCtaClick = onNavigateToHome,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        movies = uiState.movies,
        onMovieClick = onMovieClick,
    )
}
