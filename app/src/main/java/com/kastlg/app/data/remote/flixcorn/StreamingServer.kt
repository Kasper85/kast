package com.kastlg.app.data.remote.flixcorn

data class StreamingServer(
    val serverName: String,
    val quality: String,
    val language: String,
    val onlineUrl: String?,
    val directUrl: String?,
    val serverIconUrl: String?,
)
