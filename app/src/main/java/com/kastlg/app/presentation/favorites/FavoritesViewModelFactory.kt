package com.kastlg.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.domain.repositories.FavoriteRepository

class FavoritesViewModelFactory(
    private val repository: FavoriteRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FavoritesViewModel::class.java))
        return FavoritesViewModel(repository) as T
    }
}
