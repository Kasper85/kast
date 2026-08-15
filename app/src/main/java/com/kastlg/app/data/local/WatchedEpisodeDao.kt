package com.kastlg.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WatchedEpisodeDao {
    @Query(
        "SELECT EXISTS(SELECT 1 FROM watched_episodes WHERE slug = :slug AND season = :season AND episode = :episode)",
    )
    abstract fun observeExists(slug: String, season: Int, episode: Int): Flow<Boolean>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM watched_episodes WHERE slug = :slug AND season = :season AND episode = :episode)",
    )
    abstract suspend fun exists(slug: String, season: Int, episode: Int): Boolean

    @Upsert
    abstract suspend fun upsert(entity: WatchedEpisodeEntity)

    @Query("DELETE FROM watched_episodes WHERE slug = :slug AND season = :season AND episode = :episode")
    abstract suspend fun delete(slug: String, season: Int, episode: Int)

    @Transaction
    open suspend fun toggle(entity: WatchedEpisodeEntity) {
        if (exists(entity.slug, entity.season, entity.episode)) {
            delete(entity.slug, entity.season, entity.episode)
        } else {
            upsert(entity)
        }
    }
}