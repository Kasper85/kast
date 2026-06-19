package com.kastlg.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.remote.TokenStore
import com.kastlg.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState

    init {
        loadCurrentToken()
    }

    fun onInputChanged(input: String) {
        mutableUiState.update { it.copy(inputToken = input, errorMessage = null, successMessage = null) }
    }

    fun toggleTokenVisibility() {
        mutableUiState.update { it.copy(isTokenVisible = !it.isTokenVisible) }
    }

    fun saveToken() {
        val token = mutableUiState.value.inputToken.trim()
        if (token.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "Ingresa un token válido.") }
            return
        }

        viewModelScope.launch {
            tokenStore.saveToken(token)
            AppContainer.refreshTmdbRepository(token)
            loadCurrentToken()
            mutableUiState.update {
                it.copy(
                    inputToken = "",
                    successMessage = "Token guardado correctamente.",
                )
            }
        }
    }

    fun clearToken() {
        viewModelScope.launch {
            tokenStore.clearToken()
            AppContainer.refreshTmdbRepository("")
            loadCurrentToken()
            mutableUiState.update {
                it.copy(
                    inputToken = "",
                    successMessage = "Token eliminado. Configura uno nuevo para usar la app.",
                )
            }
        }
    }

    fun clearMessages() {
        mutableUiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun loadCurrentToken() {
        viewModelScope.launch {
            val token = tokenStore.getToken()
            mutableUiState.update {
                it.copy(
                    currentToken = token,
                    maskedToken = if (token.isNotEmpty()) tokenStore.maskToken(token) else "",
                    hasToken = token.isNotEmpty(),
                )
            }
        }
    }
}
