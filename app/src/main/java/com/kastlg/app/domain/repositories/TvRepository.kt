package com.kastlg.app.domain.repositories

import com.kastlg.app.data.tv.SsapClient
import com.kastlg.app.domain.models.TvConfig
import kotlinx.coroutines.flow.Flow

interface TvRepository {
    fun observeConfig(): Flow<TvConfig?>

    fun observeDiagnosticLog(): Flow<List<SsapClient.DiagnosticEntry>>

    suspend fun getConfig(): TvConfig?

    suspend fun saveConfig(config: TvConfig)

    suspend fun deleteConfig()

    suspend fun connectAndRegister(ip: String): Result<String>

    suspend fun openUrl(url: String): Result<Unit>

    suspend fun disconnect()
}
