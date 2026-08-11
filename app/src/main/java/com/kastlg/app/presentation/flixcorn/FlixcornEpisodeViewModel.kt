package com.kastlg.app.presentation.flixcorn

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.di.AppContainer
import kotlinx.coroutines.launch

data class FlixcornEpisodeUiState(
    val isLoading: Boolean = true,
    val servers: List<StreamingServer> = emptyList(),
    val selectedLanguage: String? = null,
    val error: String? = null,
    val tvSending: Boolean = false,
    val tvSuccessMessage: String? = null,
    val tvErrorMessage: String? = null,
)

class FlixcornEpisodeViewModel(
    private val slug: String,
    private val season: Int,
    private val episode: Int,
) : ViewModel() {
    private val _uiState = mutableStateOf(FlixcornEpisodeUiState())
    val uiState = _uiState

    fun loadServers() {
        viewModelScope.launch {
            _uiState.value = FlixcornEpisodeUiState(isLoading = true)
            when (val result = AppContainer.getFlixcornEpisodeServers(slug, season, episode)) {
                is FlixcornResult.Success -> {
                    _uiState.value = FlixcornEpisodeUiState(servers = result.data)
                }
                is FlixcornResult.Error -> {
                    _uiState.value = FlixcornEpisodeUiState(
                        error = "No se pudieron cargar los servidores. Intenta de nuevo.",
                    )
                }
                is FlixcornResult.Loading -> {
                    _uiState.value = FlixcornEpisodeUiState(isLoading = true)
                }
            }
        }
    }

    fun selectLanguage(language: String?) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun sendToTv(server: StreamingServer) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                tvSending = true,
                tvErrorMessage = null,
                tvSuccessMessage = null,
            )

            // Check if TV is configured
            val config = AppContainer.tvRepository.getConfig()
            if (config == null || !config.isPaired) {
                _uiState.value = _uiState.value.copy(
                    tvSending = false,
                    tvErrorMessage = "Configura tu TV primero desde Ajustes.",
                )
                return@launch
            }

            // Resolve URL and send to TV
            when (val result = AppContainer.sendFlixcornToTv(server)) {
                is FlixcornResult.Success -> {
                    val url = result.data
                    val sendResult = AppContainer.tvRepository.openUrl(url)
                    if (sendResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            tvSending = false,
                            tvSuccessMessage = "Enviado a la TV",
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            tvSending = false,
                            tvErrorMessage = sendResult.exceptionOrNull()?.message
                                ?: "Error al enviar a la TV",
                        )
                    }
                }
                is FlixcornResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        tvSending = false,
                        tvErrorMessage = "No se pudo resolver la URL del video.",
                    )
                }
                is FlixcornResult.Loading -> {}
            }
        }
    }
}

class FlixcornEpisodeViewModelFactory(
    private val slug: String,
    private val season: Int,
    private val episode: Int,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FlixcornEpisodeViewModel::class.java))
        return FlixcornEpisodeViewModel(slug = slug, season = season, episode = episode) as T
    }
}
