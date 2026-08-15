package com.kastlg.app.data.repository

import com.kastlg.app.data.local.WatchedEpisodeDao
import com.kastlg.app.data.local.WatchedEpisodeEntity
import com.kastlg.app.data.local.WatchedMovieDao
import com.kastlg.app.data.local.WatchedMovieEntity
import com.kastlg.app.domain.repositories.WatchedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomWatchedRepository(
    private val movieDao: WatchedMovieDao,
    private val episodeDao: WatchedEpisodeDao,
    private val now: () -> Long = System::currentTimeMillis,
) : WatchedRepository {
    private val toggleMutex = Mutex()

    override fun observeIsMovieWatched(movieId: Int): Flow<Boolean> = movieDao.observeExists(movieId)

    override fun observeIsEpisodeWatched(slug: String, season: Int, episode: Int): Flow<Boolean> =
        episodeDao.observeExists(slug, season, episode)

    override suspend fun toggleMovie(movieId: Int) {
        toggleMutex.withLock {
            movieDao.toggle(WatchedMovieEntity(movieId = movieId, watchedAt = now()))
        }
    }

    override suspend fun toggleEpisode(slug: String, season: Int, episode: Int) {
        toggleMutex.withLock {
            episodeDao.toggle(
                WatchedEpisodeEntity(
                    slug = slug,
                    season = season,
                    episode = episode,
                    watchedAt = now(),
                ),
            )
        }
    }
}
