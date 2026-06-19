package com.kastlg.app.presentation.history

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.domain.models.HistoryEntry
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.repositories.HistoryRepository
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
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `maps persistent history into presentation state`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        runCurrent()

        repository.values.value = listOf(
            HistoryEntry(2, "Recent", null, "", "2025-01-01", 7.5, 200L),
            HistoryEntry(1, "Older", null, "", "2024-01-01", 6.5, 100L),
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(2, 1), viewModel.uiState.value.movies.map { it.tmdbId })
    }

    @Test
    fun `persistence flow failure stops loading and exposes error`() = runTest {
        val repository = FakeHistoryRepository(failObservation = true)
        val viewModel = HistoryViewModel(repository)
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.movies.isEmpty())
        assertEquals("No se pudo cargar el historial.", viewModel.uiState.value.errorMessage)
    }

    private class FakeHistoryRepository(
        private val failObservation: Boolean = false,
    ) : HistoryRepository {
        val values = MutableStateFlow<List<HistoryEntry>>(emptyList())

        override fun observeHistory(): Flow<List<HistoryEntry>> =
            if (failObservation) {
                flow { throw java.io.IOException("History read failed") }
            } else {
                values
            }

        override suspend fun recordViewed(movie: MovieDetail) = Unit

        override suspend fun recordSentToTv(movie: MovieDetail) = Unit
    }
}
