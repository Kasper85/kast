package com.kastlg.app.presentation.flixcorn

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.local.FlixcornSeriesFavoriteEntity
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.di.AppContainer
import com.kastlg.app.domain.usecases.GetFlixcornSeriesDetail
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class FlixcornSeriesDetailUiState(
    val isLoading: Boolean = true,
    val series: FlixcornSeriesDetail? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
)

class FlixcornSeriesDetailViewModel(
    private val slug: String,
    private val getSeriesDetail: GetFlixcornSeriesDetail,
) : ViewModel() {
    private val _uiState = mutableStateOf(FlixcornSeriesDetailUiState())
    val uiState = _uiState

    fun loadSeries() {
        viewModelScope.launch {
            _uiState.value = FlixcornSeriesDetailUiState(isLoading = true)
            when (val result = getSeriesDetail(slug)) {
                is FlixcornResult.Success -> {
                    val series = result.data
                    _uiState.value = FlixcornSeriesDetailUiState(
                        isLoading = false,
                        series = series,
                        isFavorite = isFavorite(series.slug),
                    )
                }
                is FlixcornResult.Error -> {
                    _uiState.value = FlixcornSeriesDetailUiState(
                        isLoading = false,
                        error = "No se pudo cargar la serie. Intenta de nuevo.",
                    )
                }
                is FlixcornResult.Loading -> {
                    _uiState.value = FlixcornSeriesDetailUiState(isLoading = true)
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val series = uiState.value?.series
            if (series != null) {
                val dao = AppContainer.flixcornSeriesFavoriteDao
                val current = dao.observeBySlug(series.slug).firstOrNull()
                if (current != null) {
                    dao.deleteBySlug(series.slug)
                    _uiState.value = _uiState.value.copy(isFavorite = false)
                } else {
                    dao.insert(
                        FlixcornSeriesFavoriteEntity(
                            slug = series.slug,
                            title = series.title,
                            posterUrl = series.posterUrl,
                            favoritedAt = System.currentTimeMillis(),
                        ),
                    )
                    _uiState.value = _uiState.value.copy(isFavorite = true)
                }
            }
        }
    }

    private suspend fun isFavorite(slug: String): Boolean = runCatching {
        AppContainer.flixcornSeriesFavoriteDao.observeBySlug(slug)
            .map { it != null }
            .firstOrNull() ?: false
    }.getOrDefault(false)
}

class FlixcornSeriesDetailViewModelFactory(
    private val slug: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FlixcornSeriesDetailViewModel::class.java))
        return FlixcornSeriesDetailViewModel(
            slug = slug,
            getSeriesDetail = AppContainer.getFlixcornSeriesDetail,
        ) as T
    }
}