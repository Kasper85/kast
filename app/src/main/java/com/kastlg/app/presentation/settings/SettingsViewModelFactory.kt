package com.kastlg.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.data.remote.TokenStore
import com.kastlg.app.domain.repositories.TvRepository

class SettingsViewModelFactory(
    private val tokenStore: TokenStore,
    private val tvRepository: TvRepository? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
        return SettingsViewModel(tokenStore) as T
    }
}
