package com.kastlg.app.data.repository

import com.kastlg.app.data.local.FlixcornSeriesFavoriteDao
import com.kastlg.app.data.local.FlixcornSeriesFavoriteEntity
import com.kastlg.app.domain.models.FavoriteMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFlixcornSeriesFavoriteRepository(
    private val dao: FlixcornSeriesFavoriteDao,
) {
    suspend fun toggle(movie: MovieDetail) {
        viewModelScope.launch {
            val slug = movie.tmdbId.toString()
            val existing = dao.observeBySlug(slug).firstOrNull()
            if (existing != null) {
                dao.deleteBySlug(slug)
            } else {
                dao.insert(
                    FlixcornSeriesFavoriteEntity(
                        slug = slug,
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        favoritedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun observeIsFavorite(tmdbId: Int): Flow<Boolean> =
        dao.existsBySlug(tmdbId.toString())
}