package com.kastlg.app.domain.models

data class TvConfig(
    val tvIp: String,
    val tvName: String,
    val clientKey: String?,
    val isPaired: Boolean,
    /** Target type: "LG_WEBOS" or "APPLE_TV". */
    val targetType: String = "LG_WEBOS",
    /** Bridge URL for Apple TV targets. Null for LG. */
    val bridgeUrl: String? = null,
    /** RTSP port for Apple TV (default 7000). */
    val devicePort: Int = 7000,
)
