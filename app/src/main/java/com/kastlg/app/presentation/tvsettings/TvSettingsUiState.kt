package com.kastlg.app.presentation.tvsettings

import com.kastlg.app.data.tv.SsapClient
import com.kastlg.app.data.tv.discovery.DiscoveredTv
import com.kastlg.app.domain.models.TvConfig

data class TvSettingsUiState(
    val tvIp: String = "",
    val tvName: String = "",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isPairing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val config: TvConfig? = null,
    val diagnosticLog: List<SsapClient.DiagnosticEntry> = emptyList(),
    val isScanning: Boolean = false,
    val discoveredTvs: List<DiscoveredTv> = emptyList(),
)
