package com.kastlg.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kastlg.app.domain.repositories.HistoryRepository

class HistoryViewModelFactory(
    private val repository: HistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HistoryViewModel::class.java))
        return HistoryViewModel(repository) as T
    }
}
