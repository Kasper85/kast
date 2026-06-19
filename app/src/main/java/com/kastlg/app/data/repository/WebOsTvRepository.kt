package com.kastlg.app.data.repository

import android.util.Log
import com.google.gson.JsonObject
import com.kastlg.app.data.local.TvConfigDao
import com.kastlg.app.data.local.TvConfigEntity
import com.kastlg.app.data.tv.SsapClient
import com.kastlg.app.domain.models.TvConfig
import com.kastlg.app.domain.repositories.TvRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WebOsTvRepository(
    private val tvConfigDao: TvConfigDao,
    private val ssapClient: SsapClient,
) : TvRepository {

    override fun observeConfig(): Flow<TvConfig?> =
        tvConfigDao.observe().map { entity ->
            entity?.toDomain()
        }

    override fun observeDiagnosticLog(): Flow<List<SsapClient.DiagnosticEntry>> =
        ssapClient.diagnosticLog

    override suspend fun getConfig(): TvConfig? =
        tvConfigDao.get()?.toDomain()

    override suspend fun saveConfig(config: TvConfig) {
        tvConfigDao.upsert(config.toEntity())
    }

    override suspend fun deleteConfig() {
        tvConfigDao.deleteAll()
        ssapClient.disconnect()
    }

    override suspend fun connectAndRegister(ip: String): Result<String> {
        val existingConfig = tvConfigDao.get()
        val clientKey = existingConfig?.clientKey

        return try {
            val newClientKey = ssapClient.connectAndRegister(ip, clientKey)
            if (newClientKey != null) {
                // Save or update config with new client key
                val config = TvConfig(
                    tvIp = ip,
                    tvName = existingConfig?.tvName ?: "LG webOS TV",
                    clientKey = newClientKey,
                    isPaired = true,
                )
                saveConfig(config)
                Result.success(newClientKey)
            } else {
                Result.failure(Exception("Emparejamiento rechazado. Verifica que la TV esté encendida."))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("La TV no responde. Verifica que esté encendida y en la misma red."))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("No se pudo conectar a $ip. Verifica la IP y la red."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun openUrl(url: String): Result<Unit> {
        Log.d(TAG, "openUrl: url=$url, isConnected=${ssapClient.isConnected()}")

        if (!ssapClient.isConnected()) {
            Log.d(TAG, "openUrl: WebSocket no conectado, intentando reconectar...")
            val config = tvConfigDao.get()
                ?: return Result.failure(Exception("No hay TV configurada. Conectá primero."))
            val key = ssapClient.connectAndRegister(config.tvIp, config.clientKey)
            if (key == null) {
                Log.e(TAG, "openUrl: Reconexión fallida")
                return Result.failure(Exception("No se pudo conectar a la TV en ${config.tvIp}"))
            }
            Log.d(TAG, "openUrl: Reconexión exitosa")
        }

        // Usar applicationManager/launch — compatible con LG 50UQ7500 (software 04.54.40)
        val uri = "ssap://com.webos.applicationManager/launch"
        val payload = JsonObject().apply {
            addProperty("id", "com.webos.app.browser")
            val params = JsonObject()
            params.addProperty("target", url)
            add("params", params)
        }

        Log.d(TAG, "openUrl: uri=$uri")
        Log.d(TAG, "openUrl: payload=$payload")

        return try {
            val response = ssapClient.request(uri, payload)
            Log.d(TAG, "openUrl: respuesta raw=$response")

            // Verificar type=error
            val responseType = response?.type
            if (responseType == "error") {
                val errorCode = response.payload?.get("errorCode")?.asInt ?: -1
                val errorText = response.payload?.get("errorText")?.asString ?: "unknown"
                Log.e(TAG, "openUrl: ❌ type=error, errorCode=$errorCode, errorText=$errorText")
                return Result.failure(Exception("TV error $errorCode: $errorText"))
            }

            // Verificar returnValue
            val returnValue = response?.payload?.get("returnValue")?.asBoolean
            if (returnValue == false) {
                val errorCode = response?.payload?.get("errorCode")?.asInt ?: -1
                val errorText = response?.payload?.get("errorText")?.asString ?: "unknown"
                Log.e(TAG, "openUrl: ❌ returnValue=false, errorCode=$errorCode, errorText=$errorText")
                return Result.failure(Exception("TV rechazó el comando: $errorText (code $errorCode)"))
            }

            if (returnValue == true) {
                Log.d(TAG, "openUrl: ✅ returnValue=true — navegador abierto")
                return Result.success(Unit)
            }

            // Sin returnValue explícito — la TV puede no responder con returnValue para launcher
            if (response == null) {
                Log.w(TAG, "openUrl: Respuesta nula — timeout esperando respuesta")
                return Result.failure(Exception("La TV no respondió al comando"))
            }

            Log.d(TAG, "openUrl: Respuesta sin returnValue explícito: type=$responseType")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "openUrl: Excepción: ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        ssapClient.disconnect()
    }

    private fun TvConfigEntity.toDomain() = TvConfig(
        tvIp = tvIp,
        tvName = tvName,
        clientKey = clientKey,
        isPaired = isPaired,
    )

    private fun TvConfig.toEntity() = TvConfigEntity(
        id = 1,
        tvIp = tvIp,
        tvName = tvName,
        clientKey = clientKey,
        isPaired = isPaired,
    )

    private companion object {
        const val TAG = "WebOsTvRepository"
    }
}
