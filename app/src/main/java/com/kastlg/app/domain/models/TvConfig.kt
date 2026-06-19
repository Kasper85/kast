package com.kastlg.app.domain.models

data class TvConfig(
    val tvIp: String,
    val tvName: String,
    val clientKey: String?,
    val isPaired: Boolean,
)
