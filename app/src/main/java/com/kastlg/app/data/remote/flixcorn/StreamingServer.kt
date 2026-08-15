package com.kastlg.app.data.remote.flixcorn

data class StreamingServer(
    val serverName: String,
    val quality: String,
    val language: String,
    val onlineUrl: String?,
    val directUrl: String?,
    val serverIconUrl: String?,
)

/**
 * Orders servers online-first: servers with a `/player/` [onlineUrl] rank first
 * ("Ver Online"), followed by servers with only an `/external/` [directUrl]
 * ("Link Directo"). Final tiebreak by [serverName] (case-insensitive).
 */
fun List<StreamingServer>.sortedOnlineFirst(): List<StreamingServer> = sortedWith(
    compareByDescending<StreamingServer> { it.onlineUrl?.contains("/player/") == true }
        .thenByDescending { it.directUrl?.contains("/external/") == true }
        .thenBy { it.serverName.lowercase() },
)
