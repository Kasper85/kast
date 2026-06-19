package com.kastlg.app.presentation.favorites

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.domain.models.FavoriteMovie
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.repositories.FavoriteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `maps persistent favorites into presentation state`() = runTest {
        val repository = FakeFavoriteRepository()
        val viewModel = FavoritesViewModel(repository)
        runCurrent()

        repository.values.value = listOf(
            FavoriteMovie(2, "Second", null, "", "2025-01-01", 7.5, 200L),
            FavoriteMovie(1, "First", null, "", "2024-01-01", 6.5, 100L),
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(2, 1), viewModel.uiState.value.movies.map { it.tmdbId })
    }

    @Test
    fun `persistence flow failure stops loading and exposes error`() = runTest {
        val repository = FakeFavoriteRepository(failObservation = true)
        val viewModel = FavoritesViewModel(repository)
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.movies.isEmpty())
        assertEquals("No se pudieron cargar los favoritos.", viewModel.uiState.value.errorMessage)
    }

    private class FakeFavoriteRepository(
        private val failObservation: Boolean = false,
    ) : FavoriteRepository {
        val values = MutableStateFlow<List<FavoriteMovie>>(emptyList())

        override fun observeFavorites(): Flow<List<FavoriteMovie>> =
            if (failObservation) {
                flow { throw java.io.IOException("Favorites read failed") }
            } else {
                values
            }

        override fun observeIsFavorite(tmdbId: Int): Flow<Boolean> =
            MutableStateFlow(false)

        override suspend fun toggle(movie: MovieDetail) = Unit
    }
}
