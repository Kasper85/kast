package com.kastlg.app.presentation.tvsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.tv.discovery.DiscoveredTv
import com.kastlg.app.data.tv.discovery.TvDiscoveryManager
import com.kastlg.app.domain.repositories.TvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TvSettingsViewModel(
    private val tvRepository: TvRepository,
    private val discoveryManager: TvDiscoveryManager = TvDiscoveryManager(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TvSettingsUiState())
    val uiState: StateFlow<TvSettingsUiState> = mutableUiState

    init {
        observeConfig()
        observeDiagnosticLog()
        autoReconnect()
    }

    private fun autoReconnect() {
        viewModelScope.launch {
            val config = tvRepository.getConfig()
            if (config != null && config.isPaired && config.clientKey != null) {
                mutableUiState.update { it.copy(isConnecting = true) }
                val result = tvRepository.connectAndRegister(config.tvIp)
                result.fold(
                    onSuccess = {
                        mutableUiState.update {
                            it.copy(isConnecting = false, isConnected = true)
                        }
                    },
                    onFailure = {
                        mutableUiState.update {
                            it.copy(isConnecting = false, isConnected = false)
                        }
                    },
                )
            }
        }
    }

    fun onIpChanged(ip: String) {
        mutableUiState.update { it.copy(tvIp = ip, errorMessage = null) }
    }

    fun onNameChanged(name: String) {
        mutableUiState.update { it.copy(tvName = name) }
    }

    fun discoverTvs() {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isScanning = true, discoveredTvs = emptyList(), errorMessage = null)
            }
            val tvs = discoveryManager.scan()
            mutableUiState.update {
                it.copy(
                    isScanning = false,
                    discoveredTvs = tvs,
                    errorMessage = if (tvs.isEmpty()) "No se encontraron TVs en la red" else null,
                )
            }
        }
    }

    fun selectDiscoveredTv(tv: DiscoveredTv) {
        mutableUiState.update {
            it.copy(
                tvIp = tv.ip,
                tvName = tv.name,
                discoveredTvs = emptyList(),
            )
        }
    }

    fun connect() {
        val ip = mutableUiState.value.tvIp.trim()
        if (ip.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "Ingresa la IP de la TV.") }
            return
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isConnecting = true,
                    isPairing = false,
                    errorMessage = null,
                    successMessage = null,
                )
            }

            val result = tvRepository.connectAndRegister(ip)
            result.fold(
                onSuccess = { clientKey ->
                    mutableUiState.update {
                        it.copy(
                            isConnecting = false,
                            isPairing = false,
                            isConnected = true,
                            successMessage = "TV conectada",
                        )
                    }
                },
                onFailure = { error ->
                    val msg = error.message ?: "Error de conexión"
                    val isPairing = msg.contains("accept", ignoreCase = true) ||
                        msg.contains("prompt", ignoreCase = true)
                    mutableUiState.update {
                        it.copy(
                            isConnecting = false,
                            isPairing = isPairing,
                            isConnected = false,
                            errorMessage = if (isPairing) {
                                "Acepta la conexión en la pantalla de la TV."
                            } else {
                                msg
                            },
                        )
                    }
                },
            )
        }
    }

    fun clearMessages() {
        mutableUiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun deleteConfig() {
        viewModelScope.launch {
            tvRepository.deleteConfig()
            mutableUiState.update {
                it.copy(
                    isConnected = false,
                    config = null,
                    successMessage = "Configuración eliminada",
                )
            }
        }
    }

    private fun observeConfig() {
        viewModelScope.launch {
            tvRepository.observeConfig().collect { config ->
                mutableUiState.update {
                    it.copy(
                        config = config,
                        tvIp = config?.tvIp ?: it.tvIp,
                        tvName = config?.tvName ?: it.tvName,
                        isConnected = config?.isPaired == true,
                    )
                }
            }
        }
    }

    private fun observeDiagnosticLog() {
        viewModelScope.launch {
            tvRepository.observeDiagnosticLog().collect { log ->
                mutableUiState.update { it.copy(diagnosticLog = log) }
            }
        }
    }
}
