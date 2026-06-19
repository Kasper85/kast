package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY viewed_at DESC, tmdb_id DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Upsert
    suspend fun upsert(entry: HistoryEntity)

    @Query("UPDATE history SET sent_to_tv = 1, viewed_at = :viewedAt WHERE tmdb_id = :tmdbId")
    suspend fun markSentToTv(tmdbId: Int, viewedAt: Long)
}
