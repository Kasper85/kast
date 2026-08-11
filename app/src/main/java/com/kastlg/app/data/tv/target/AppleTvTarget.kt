package com.kastlg.app.data.tv.target

import android.util.Log
import com.kastlg.app.data.tv.apple.AppleTVBridgeClient

/**
 * Apple TV playback target via local AirPlay bridge.
 *
 * Communicates with the Python FastAPI bridge over HTTP.
 * The bridge handles all AirPlay 1 RTSP protocol with the Apple TV.
 *
 * Architecture:
 *   Kast Android → HTTP → Python Bridge → RTSP → Apple TV 3
 */
class AppleTvTarget(
    private val bridgeClient: AppleTVBridgeClient,
    override val targetId: String,
    override val displayName: String = "Apple TV",
    private val devicePort: Int = 7000,
) : PlaybackTarget {

    private var _isConnected = false

    override val targetType: TargetType = TargetType.APPLE_TV

    override val isConnected: Boolean
        get() = _isConnected

    override suspend fun connect(): Result<Unit> {
        return try {
            val healthy = bridgeClient.healthCheck()
            if (healthy) {
                _isConnected = true
                Log.d(TAG, "Bridge reachable at ${bridgeClient}")
                Result.success(Unit)
            } else {
                _isConnected = false
                Result.failure(
                    Exception(
                        "No se pudo conectar al bridge AirPlay. " +
                            "Verifica que el bridge esté corriendo en la misma red."
                    )
                )
            }
        } catch (e: Exception) {
            _isConnected = false
            Log.e(TAG, "Connect failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun openUrl(url: String): Result<Unit> {
        if (!_isConnected) {
            return Result.failure(
                Exception("Bridge no disponible. Conectá primero.")
            )
        }

        return try {
            val result = bridgeClient.play(
                url = url,
                deviceIp = targetId,
                devicePort = devicePort,
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        Log.d(TAG, "Playback started: ${response.message}")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(response.message))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Play failed: ${error.message}")
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Play exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun stop(): Result<Unit> {
        if (!_isConnected) return Result.success(Unit)

        return try {
            val result = bridgeClient.stop(targetId)
            result.fold(
                onSuccess = { response ->
                    Log.d(TAG, "Playback stopped: ${response.message}")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.w(TAG, "Stop failed: ${error.message}")
                    Result.failure(error)
                },
            )
        } catch (e: Exception) {
            Log.w(TAG, "Stop exception: ${e.message}")
            Result.failure(e)
        }
    }

    override fun disconnect() {
        _isConnected = false
    }

    private companion object {
        const val TAG = "AppleTvTarget"
    }
}
