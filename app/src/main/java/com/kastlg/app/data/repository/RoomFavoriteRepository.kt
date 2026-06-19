package com.kastlg.app.data.repository

import com.kastlg.app.data.local.FavoriteDao
import com.kastlg.app.data.local.FavoriteEntity
import com.kastlg.app.domain.models.FavoriteMovie
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.repositories.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomFavoriteRepository(
    private val dao: FavoriteDao,
    private val now: () -> Long = System::currentTimeMillis,
) : FavoriteRepository {
    private val toggleMutex = Mutex()

    override fun observeFavorites(): Flow<List<FavoriteMovie>> =
        dao.observeAll().map { favorites -> favorites.map(FavoriteEntity::toDomain) }

    override fun observeIsFavorite(tmdbId: Int): Flow<Boolean> = dao.observeExists(tmdbId)

    override suspend fun toggle(movie: MovieDetail) {
        toggleMutex.withLock {
            dao.toggle(movie.toFavoriteEntity(now()))
        }
    }
}

private fun FavoriteEntity.toDomain(): FavoriteMovie = FavoriteMovie(
    tmdbId = tmdbId,
    title = title,
    posterUrl = posterUrl,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    favoritedAt = favoritedAt,
)

private fun MovieDetail.toFavoriteEntity(favoritedAt: Long): FavoriteEntity = FavoriteEntity(
    tmdbId = id,
    title = title,
    posterUrl = posterUrl,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    favoritedAt = favoritedAt,
)
