package com.kastlg.app.presentation.flixcorn

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FlixcornHomeUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<FlixcornSearchResult> = emptyList(),
    val errorMessage: String? = null,
)

class FlixcornHomeViewModel : ViewModel() {
    private val _uiState = mutableStateOf(FlixcornHomeUiState())
    val uiState = _uiState
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, errorMessage = null)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false)
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(400)
            when (val result = AppContainer.searchFlixcorn(query)) {
                is FlixcornResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        results = result.data,
                        isLoading = false,
                    )
                }
                is FlixcornResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
                is FlixcornResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al buscar. Intenta de nuevo.",
                    )
                }
            }
        }
    }
}

class FlixcornHomeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FlixcornHomeViewModel::class.java))
        return FlixcornHomeViewModel() as T
    }
}
