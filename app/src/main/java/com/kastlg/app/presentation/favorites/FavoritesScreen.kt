package com.kastlg.app.presentation.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kastlg.app.di.AppContainer
import com.kastlg.app.presentation.library.SavedMoviesScreen

@Composable
fun FavoritesRoute(
    onMovieClick: (Int) -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: FavoritesViewModel = viewModel(
        factory = FavoritesViewModelFactory(AppContainer.favoriteRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SavedMoviesScreen(
        title = "Favoritos",
        emptyMessage = "No tienes favoritos todavía",
        emptyCtaLabel = "Explorar películas",
        onEmptyCtaClick = onNavigateToHome,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        movies = uiState.movies,
        onMovieClick = onMovieClick,
    )
}
