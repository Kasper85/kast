package com.kastlg.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kastlg.app.data.local.FlixcornEpisodeCacheDao
import com.kastlg.app.data.local.FlixcornEpisodeCacheEntity
import com.kastlg.app.data.local.FlixcornSeriesDao
import com.kastlg.app.data.local.FlixcornSeriesEntity
import com.kastlg.app.data.remote.flixcorn.FlixcornError
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.FlixcornScraper
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.data.remote.flixcorn.sortedOnlineFirst
import com.kastlg.app.domain.repositories.FlixcornRepository

class FlixcornRepositoryImpl(
    private val scraper: FlixcornScraper,
    private val seriesDao: FlixcornSeriesDao,
    private val episodeCacheDao: FlixcornEpisodeCacheDao,
    private val gson: Gson = Gson(),
) : FlixcornRepository {

    override suspend fun searchSeries(query: String): FlixcornResult<List<FlixcornSearchResult>> {
        return try {
            scraper.searchSeries(query)
        } catch (e: Exception) {
            Log.e(TAG, "searchSeries failed: ${e.message}")
            FlixcornResult.Error(FlixcornError.UNREACHABLE)
        }
    }

    override suspend fun getSeriesDetail(slug: String): FlixcornResult<FlixcornSeriesDetail> {
        val cached = seriesDao.getBySlug(slug)
        if (cached != null) {
            Log.d(TAG, "getSeriesDetail cache hit slug=$slug")
            val detail = cached.toDomainDetail()
            if (detail != null) {
                return FlixcornResult.Success(detail)
            }
        }

        return when (val result = scraper.getSeriesDetail(slug)) {
            is FlixcornResult.Success -> {
                val series = result.data
                seriesDao.insert(series.toEntity())
                FlixcornResult.Success(series)
            }
            is FlixcornResult.Error -> result
            is FlixcornResult.Loading -> result
        }
    }

    override suspend fun getEpisodeServers(
        slug: String,
        season: Int,
        episode: Int,
    ): FlixcornResult<List<StreamingServer>> {
        val episodeUrl = "$BASE_URL/ver/$slug/temporada-$season/capitulo-$episode.html"
        val cached = episodeCacheDao.getByUrl(episodeUrl)
        if (cached != null) {
            Log.d(TAG, "getEpisodeServers cache hit slug=$slug s${season}e$episode")
            val servers = parseServersFromJson(cached.serversJson)
            if (servers.isNotEmpty()) {
                // Legacy caches were stored in name-priority order — re-sort online-first.
                return FlixcornResult.Success(servers.sortedOnlineFirst())
            }
        }

        return when (val result = scraper.getEpisodeServers(slug, season, episode)) {
            is FlixcornResult.Success -> {
                val serversJson = gson.toJson(result.data)
                episodeCacheDao.insert(
                    FlixcornEpisodeCacheEntity(
                        episodeUrl = episodeUrl,
                        serversJson = serversJson,
                        cachedAt = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + EPISODE_CACHE_TTL_MS,
                    ),
                )
                FlixcornResult.Success(result.data)
            }
            is FlixcornResult.Error -> result
            is FlixcornResult.Loading -> result
        }
    }

    override suspend fun resolvePlayerUrl(token: String): FlixcornResult<String> {
        return scraper.resolvePlayerUrl(token)
    }

    private fun parseServersFromJson(json: String): List<StreamingServer> {
        return try {
            val type = object : TypeToken<List<StreamingServer>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cached servers JSON")
            emptyList()
        }
    }

    private fun FlixcornSeriesDetail.toEntity(): FlixcornSeriesEntity {
        return FlixcornSeriesEntity(
            slug = slug,
            title = title,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            overview = overview,
            year = year,
            rating = rating,
            genres = gson.toJson(genres),
            numberOfSeasons = numberOfSeasons,
            numberOfEpisodes = numberOfEpisodes,
            status = status,
            detailUrl = "$BASE_URL/serie/$slug.html",
            cachedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + SERIES_CACHE_TTL_MS,
        )
    }

    private fun FlixcornSeriesEntity.toDomainDetail(): FlixcornSeriesDetail? {
        return try {
            val genresList: List<String> = gson.fromJson(
                genres,
                object : TypeToken<List<String>>() {}.type,
            )
            FlixcornSeriesDetail(
                slug = slug,
                title = title,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                overview = overview,
                year = year,
                rating = rating,
                genres = genresList,
                numberOfSeasons = numberOfSeasons,
                numberOfEpisodes = numberOfEpisodes,
                status = status,
                seasons = emptyList(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert cached entity to domain: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "FlixcornRepo"
        private const val BASE_URL = "https://www.flixcorn.net"
        private const val SERIES_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        private const val EPISODE_CACHE_TTL_MS = 60 * 60 * 1000L
    }
}
