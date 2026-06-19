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
)
