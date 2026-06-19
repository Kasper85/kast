package com.kastlg.app.domain.repositories

import com.kastlg.app.domain.models.HistoryEntry
import com.kastlg.app.domain.models.MovieDetail
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeHistory(): Flow<List<HistoryEntry>>

    suspend fun recordViewed(movie: MovieDetail)

    suspend fun recordSentToTv(movie: MovieDetail)
}
