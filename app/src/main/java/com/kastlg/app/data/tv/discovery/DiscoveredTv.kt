package com.kastlg.app.data.tv.discovery

data class DiscoveredTv(
    val ip: String,
    val name: String,
    val modelName: String = "",
    val port: Int = 3001,
    val isSsl: Boolean = true,
)
