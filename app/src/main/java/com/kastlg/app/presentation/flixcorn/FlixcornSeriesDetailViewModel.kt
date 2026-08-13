package com.kastlg.app.presentation.flixcorn

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlixcornSeriesDetailUiState(
    val isLoading: Boolean = true,
    val series: FlixcornSeriesDetail? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
)

class FlixcornSeriesDetailViewModel(
    private val slug: String,
) : ViewModel() {
    private val _uiState = mutableStateOf(FlixcornSeriesDetailUiState())
    val uiState = _uiState

    private val flixcornSeriesFavoriteRepository = AppContainer.flixcornSeriesFavoriteRepository

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                val series = uiState.value?.series
                if (series != null) {
                    flixcornSeriesFavoriteRepository.toggle(series)
                    _uiState.value = _uiState.value.copy(isFavorite = !uiState.value.isFavorite)
                }
            }
        }
    }

    fun loadSeries() {
        viewModelScope.launch {
            _uiState.value = FlixcornSeriesDetailUiState(isLoading = true)
            when (val result = AppContainer.getFlixcornSeriesDetail(slug)) {
                is FlixcornResult.Success -> {
                    _uiState.value = FlixcornSeriesDetailUiState(series = result.data, isFavorite = isFavorite(result.data.slug))
                }
                is FlixcornResult.Error -> {
                    _uiState.value = FlixcornSeriesDetailUiState(
                        error = "No se pudo cargar la serie. Intenta de nuevo.",
                    )
                }
                is FlixcornResult.Loading -> {
                    _uiState.value = FlixcornSeriesDetailUiState(isLoading = true)
                }
            }
        }
    }

    private fun isFavorite(slug: String): Boolean {
        return flixcornSeriesFavoriteRepository.observeIsFavorite(slug.toInt()).firstOrDefault(false)
    }
}

class FlixcornSeriesDetailViewModelFactory(
    private val slug: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FlixcornSeriesDetailViewModel::class.java))
        return FlixcornSeriesDetailViewModel(slug = slug) as T
    }
}
