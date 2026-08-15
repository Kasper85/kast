package com.kastlg.app.domain.repositories

import com.kastlg.app.domain.models.FavoriteMovie
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.models.TvShowDetail
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<FavoriteMovie>>

    fun observeIsFavorite(tmdbId: Int): Flow<Boolean>

    suspend fun toggle(movie: MovieDetail)

    suspend fun toggleTvShow(tvShow: TvShowDetail) = Unit
}
