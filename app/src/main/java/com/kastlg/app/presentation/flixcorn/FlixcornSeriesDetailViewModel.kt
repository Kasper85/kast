package com.kastlg.app.presentation.flixcorn

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.di.AppContainer
import kotlinx.coroutines.launch

data class FlixcornSeriesDetailUiState(
    val isLoading: Boolean = true,
    val series: FlixcornSeriesDetail? = null,
    val error: String? = null,
)

class FlixcornSeriesDetailViewModel(
    private val slug: String,
) : ViewModel() {
    private val _uiState = mutableStateOf(FlixcornSeriesDetailUiState())
    val uiState = _uiState

    fun loadSeries() {
        viewModelScope.launch {
            _uiState.value = FlixcornSeriesDetailUiState(isLoading = true)
            when (val result = AppContainer.getFlixcornSeriesDetail(slug)) {
                is FlixcornResult.Success -> {
                    _uiState.value = FlixcornSeriesDetailUiState(series = result.data)
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
