package com.kastlg.app.data.repository

import com.kastlg.app.data.local.HistoryDao
import com.kastlg.app.data.local.HistoryEntity
import com.kastlg.app.domain.models.HistoryEntry
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.repositories.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHistoryRepository(
    private val dao: HistoryDao,
    private val now: () -> Long = System::currentTimeMillis,
) : HistoryRepository {
    override fun observeHistory(): Flow<List<HistoryEntry>> =
        dao.observeAll().map { entries -> entries.map(HistoryEntity::toDomain) }

    override suspend fun recordViewed(movie: MovieDetail) {
        dao.upsert(movie.toHistoryEntity(now()))
    }

    override suspend fun recordSentToTv(movie: MovieDetail) {
        val viewedAt = now()
        dao.upsert(movie.toHistoryEntity(viewedAt))
        dao.markSentToTv(movie.id, viewedAt)
    }
}

private fun HistoryEntity.toDomain(): HistoryEntry = HistoryEntry(
    tmdbId = tmdbId,
    title = title,
    posterUrl = posterUrl,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    viewedAt = viewedAt,
    sentToTv = sentToTv,
)

private fun MovieDetail.toHistoryEntity(viewedAt: Long): HistoryEntity = HistoryEntity(
    tmdbId = id,
    title = title,
    posterUrl = posterUrl,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    viewedAt = viewedAt,
)
