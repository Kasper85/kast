package com.kastlg.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_config")
data class TvConfigEntity(
    @PrimaryKey
    val id: Int = 1, // Singleton: single TV config
    @ColumnInfo(name = "tv_ip")
    val tvIp: String,
    @ColumnInfo(name = "tv_name")
    val tvName: String,
    @ColumnInfo(name = "client_key")
    val clientKey: String?,
    @ColumnInfo(name = "is_paired")
    val isPaired: Boolean,
    /** Target type: "LG_WEBOS" or "APPLE_TV". Defaults to "LG_WEBOS" for backward compat. */
    @ColumnInfo(name = "target_type", defaultValue = "LG_WEBOS")
    val targetType: String = "LG_WEBOS",
    /** Bridge URL for Apple TV targets (e.g., "http://192.168.1.10:8420"). Null for LG. */
    @ColumnInfo(name = "bridge_url")
    val bridgeUrl: String? = null,
    /** RTSP port for Apple TV (default 7000). LG uses WebSocket port 3001. */
    @ColumnInfo(name = "device_port", defaultValue = "7000")
    val devicePort: Int = 7000,
)
