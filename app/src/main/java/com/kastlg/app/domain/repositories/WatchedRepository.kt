package com.kastlg.app.domain.repositories

import kotlinx.coroutines.flow.Flow

interface WatchedRepository {
    fun observeIsMovieWatched(movieId: Int): Flow<Boolean>

    fun observeIsEpisodeWatched(slug: String, season: Int, episode: Int): Flow<Boolean>

    suspend fun toggleMovie(movieId: Int)

    suspend fun toggleEpisode(slug: String, season: Int, episode: Int)
}
