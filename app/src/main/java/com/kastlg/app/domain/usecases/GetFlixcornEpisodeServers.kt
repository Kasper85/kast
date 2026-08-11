package com.kastlg.app.domain.usecases

import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.domain.repositories.FlixcornRepository

class GetFlixcornEpisodeServers(
    private val repository: FlixcornRepository,
) {
    suspend operator fun invoke(
        slug: String,
        season: Int,
        episode: Int,
    ): FlixcornResult<List<StreamingServer>> {
        return repository.getEpisodeServers(slug, season, episode)
    }
}
