package com.kastlg.app.presentation.settings

data class SettingsUiState(
    val currentToken: String = "",
    val maskedToken: String = "",
    val inputToken: String = "",
    val hasToken: Boolean = false,
    val isTokenVisible: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)
