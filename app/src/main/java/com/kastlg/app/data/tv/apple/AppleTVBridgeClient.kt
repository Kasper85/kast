package com.kastlg.app.data.tv.apple

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * HTTP client for communicating with the Kast AirPlay bridge.
 *
 * The bridge is a local Python FastAPI server that translates HTTP requests
 * into AirPlay 1 RTSP communication with Apple TV 3 (A1469).
 *
 * Default bridge URL: http://<bridge-ip>:8420/api/v1
 *
 * Bridge API:
 *   GET  /devices         — list discovered devices
 *   POST /devices/discover — trigger discovery
 *   POST /play            — start playback
 *   POST /stop            — stop playback
 */
class AppleTVBridgeClient(
    private val bridgeBaseUrl: String,
    private val timeoutSeconds: Long = 10,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Check if the bridge is reachable.
     */
    suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$bridgeBaseUrl/health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bridge health check failed: ${e.message}")
            false
        }
    }

    /**
     * Discover AirPlay devices on the network.
     */
    suspend fun discoverDevices(): Result<List<BridgeDeviceInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$bridgeBaseUrl/devices/discover")
                .post("".toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Discovery failed: ${response.code}")
                    )
                }

                val body = response.body?.string() ?: "{}"
                val parsed = gson.fromJson(body, DiscoverResponse::class.java)
                Result.success(parsed.devices)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Device discovery failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Add a device manually by IP.
     */
    suspend fun addDevice(ip: String, port: Int = 7000): Result<BridgeDeviceInfo> = withContext(Dispatchers.IO) {
        try {
            val deviceRequest = BridgeDeviceInfo(
                name = "Apple TV ($ip)",
                ip = ip,
                port = port,
                model = "AppleTV3,2",
            )
            val json = gson.toJson(deviceRequest)

            val request = Request.Builder()
                .url("$bridgeBaseUrl/devices/add")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Add device failed: ${response.code}")
                    )
                }

                val body = response.body?.string() ?: "{}"
                val device = gson.fromJson(body, BridgeDeviceInfo::class.java)
                Result.success(device)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Add device failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Start AirPlay playback on a device.
     *
     * @param url Media URL to play
     * @param deviceIp Apple TV IP address
     * @param devicePort AirPlay RTSP port (default 7000)
     * @param title Optional media title
     */
    suspend fun play(
        url: String,
        deviceIp: String,
        devicePort: Int = 7000,
        title: String? = null,
    ): Result<BridgePlayResponse> = withContext(Dispatchers.IO) {
        try {
            val playRequest = BridgePlayRequest(
                url = url,
                deviceIp = deviceIp,
                devicePort = devicePort,
                title = title,
            )
            val json = gson.toJson(playRequest)

            val request = Request.Builder()
                .url("$bridgeBaseUrl/play")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"

                if (!response.isSuccessful) {
                    val error = gson.fromJson(body, BridgeErrorResponse::class.java)
                    return@withContext Result.failure(
                        Exception(error.detail ?: "Play failed: ${response.code}")
                    )
                }

                val result = gson.fromJson(body, BridgePlayResponse::class.java)
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Play failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Stop AirPlay playback on a device.
     */
    suspend fun stop(deviceIp: String): Result<BridgePlayResponse> = withContext(Dispatchers.IO) {
        try {
            val stopRequest = BridgeStopRequest(deviceIp = deviceIp)
            val json = gson.toJson(stopRequest)

            val request = Request.Builder()
                .url("$bridgeBaseUrl/stop")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"

                if (!response.isSuccessful) {
                    val error = gson.fromJson(body, BridgeErrorResponse::class.java)
                    return@withContext Result.failure(
                        Exception(error.detail ?: "Stop failed: ${response.code}")
                    )
                }

                val result = gson.fromJson(body, BridgePlayResponse::class.java)
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * List currently active playback sessions.
     */
    suspend fun listSessions(): Result<List<BridgeSessionInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$bridgeBaseUrl/sessions")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("List sessions failed: ${response.code}")
                    )
                }

                val body = response.body?.string() ?: "[]"
                val sessions = gson.fromJson(body, Array<BridgeSessionInfo>::class.java).toList()
                Result.success(sessions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "List sessions failed: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Data classes for bridge API ---

    data class BridgeDeviceInfo(
        val name: String = "",
        val ip: String = "",
        val port: Int = 7000,
        val model: String = "",
        @SerializedName("device_type")
        val deviceType: String = "apple_tv",
    )

    data class BridgePlayRequest(
        val url: String,
        @SerializedName("device_ip")
        val deviceIp: String,
        @SerializedName("device_port")
        val devicePort: Int = 7000,
        val title: String? = null,
    )

    data class BridgeStopRequest(
        @SerializedName("device_ip")
        val deviceIp: String,
    )

    data class BridgePlayResponse(
        val success: Boolean,
        val message: String,
        val data: Map<String, Any>? = null,
    )

    data class BridgeErrorResponse(
        val detail: String? = null,
    )

    data class BridgeSessionInfo(
        @SerializedName("session_id")
        val sessionId: String,
        @SerializedName("device_ip")
        val deviceIp: String,
        val state: String,
    )

    data class DiscoverResponse(
        val devices: List<BridgeDeviceInfo> = emptyList(),
    )

    private companion object {
        const val TAG = "AppleTVBridgeClient"
    }
}
