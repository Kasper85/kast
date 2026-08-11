package com.kastlg.app.data.tv.target

/**
 * Types of playback targets supported by Kast.
 */
enum class TargetType {
    /** LG webOS TV — direct SSAP/WebSocket communication */
    LG_WEBOS,

    /** Apple TV — via local AirPlay bridge (Python FastAPI) */
    APPLE_TV,
}
