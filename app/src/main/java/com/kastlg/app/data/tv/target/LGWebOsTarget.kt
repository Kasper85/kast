package com.kastlg.app.data.tv.target

import com.kastlg.app.data.tv.SsapClient

/**
 * LG webOS TV playback target.
 *
 * Wraps the existing [SsapClient] to provide the [PlaybackTarget] interface.
 * Connection is handled via SSAP WebSocket on port 3001 (SSL).
 * Playback opens the URL in the TV's built-in browser.
 */
class LGWebOsTarget(
    private val ssapClient: SsapClient,
    override val targetId: String,
    override val displayName: String = "LG webOS TV",
) : PlaybackTarget {

    override val targetType: TargetType = TargetType.LG_WEBOS

    override val isConnected: Boolean
        get() = ssapClient.isConnected()

    override suspend fun connect(): Result<Unit> {
        return try {
            val clientKey = ssapClient.connectAndRegister(targetId)
            if (clientKey != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pairing rechazado. Verifica que la TV esté encendida."))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("La TV no responde. Verifica que esté encendida y en la misma red."))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("No se pudo conectar a $targetId. Verifica la IP y la red."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun openUrl(url: String): Result<Unit> {
        if (!ssapClient.isConnected()) {
            return Result.failure(Exception("No hay conexión con la TV. Conectá primero."))
        }

        val uri = "ssap://com.webos.applicationManager/launch"
        val payload = com.google.gson.JsonObject().apply {
            addProperty("id", "com.webos.app.browser")
            val params = com.google.gson.JsonObject()
            params.addProperty("target", url)
            add("params", params)
        }

        return try {
            val response = ssapClient.request(uri, payload)

            val responseType = response?.type
            if (responseType == "error") {
                val errorCode = response.payload?.get("errorCode")?.asInt ?: -1
                val errorText = response.payload?.get("errorText")?.asString ?: "unknown"
                return Result.failure(Exception("TV error $errorCode: $errorText"))
            }

            val returnValue = response?.payload?.get("returnValue")?.asBoolean
            if (returnValue == false) {
                val errorCode = response?.payload?.get("errorCode")?.asInt ?: -1
                val errorText = response?.payload?.get("errorText")?.asString ?: "unknown"
                return Result.failure(Exception("TV rechazó el comando: $errorText (code $errorCode)"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stop(): Result<Unit> {
        // LG webOS doesn't need explicit stop — browser manages playback
        return Result.success(Unit)
    }

    override fun disconnect() {
        ssapClient.disconnect()
    }
}
