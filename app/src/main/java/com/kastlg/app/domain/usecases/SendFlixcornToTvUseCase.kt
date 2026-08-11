package com.kastlg.app.domain.usecases

import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.domain.repositories.FlixcornRepository

class SendFlixcornToTvUseCase(
    private val repository: FlixcornRepository,
) {
    suspend operator fun invoke(server: StreamingServer): FlixcornResult<String> {
        val url = server.onlineUrl ?: server.directUrl
            ?: return FlixcornResult.Error(com.kastlg.app.data.remote.flixcorn.FlixcornError.NO_SERVERS_FOUND)

        val token = extractToken(url) ?: return FlixcornResult.Success(url)
        return repository.resolvePlayerUrl(token)
    }

    private fun extractToken(url: String): String? {
        val match = Regex("/(player|external)/([a-f0-9]+)").find(url)
        return match?.groupValues?.get(2)
    }
}
