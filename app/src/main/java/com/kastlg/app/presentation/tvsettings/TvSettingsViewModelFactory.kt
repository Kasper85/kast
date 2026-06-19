package com.kastlg.app.presentation.tvsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.domain.repositories.TvRepository

class TvSettingsViewModelFactory(
    private val tvRepository: TvRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TvSettingsViewModel::class.java))
        return TvSettingsViewModel(tvRepository) as T
    }
}
