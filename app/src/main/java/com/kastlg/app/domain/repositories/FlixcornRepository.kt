package com.kastlg.app.domain.repositories

import com.kastlg.app.data.remote.flixcorn.FlixcornError
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.StreamingServer

interface FlixcornRepository {
    suspend fun searchSeries(query: String): FlixcornResult<List<FlixcornSearchResult>>
    suspend fun getSeriesDetail(slug: String): FlixcornResult<FlixcornSeriesDetail>
    suspend fun getEpisodeServers(
        slug: String,
        season: Int,
        episode: Int,
    ): FlixcornResult<List<StreamingServer>>
    suspend fun resolvePlayerUrl(token: String): FlixcornResult<String>
}
