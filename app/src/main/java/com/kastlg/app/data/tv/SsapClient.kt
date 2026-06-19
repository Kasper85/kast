package com.kastlg.app.data.tv

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * SSAP WebSocket client for LG webOS TV communication.
 * Matches the working test-lg.js handshake exactly:
 * - wss://IP:3001 (SSL)
 * - rejectUnauthorized: false equivalent
 * - Exact manifest structure from test-lg.js
 */
class SsapClient {
    private val okHttpClient = createSslClient()

    private var webSocket: WebSocket? = null
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<SsapMessage?>>()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _pairingEvent = MutableSharedFlow<PairingEvent>()
    val pairingEvent: SharedFlow<PairingEvent> = _pairingEvent.asSharedFlow()

    private val _diagnosticLog = MutableStateFlow<List<DiagnosticEntry>>(emptyList())
    val diagnosticLog: StateFlow<List<DiagnosticEntry>> = _diagnosticLog.asStateFlow()

    data class DiagnosticEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val step: String,
        val detail: String,
        val isOk: Boolean = true,
    )

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        REGISTERING,
        WAITING_USER_PROMPT,
        CONNECTED,
        ERROR,
    }

    sealed class PairingEvent {
        data object WaitingForUser : PairingEvent()
        data class Success(val clientKey: String) : PairingEvent()
        data class Error(val message: String) : PairingEvent()
    }

    private fun log(step: String, detail: String, isOk: Boolean = true) {
        Log.d(TAG, "[$step] $detail")
        _diagnosticLog.value = _diagnosticLog.value + DiagnosticEntry(
            step = step,
            detail = detail,
            isOk = isOk,
        )
    }

    private fun logError(step: String, detail: String) {
        Log.e(TAG, "[$step] $detail")
        _diagnosticLog.value = _diagnosticLog.value + DiagnosticEntry(
            step = step,
            detail = detail,
            isOk = false,
        )
    }

    fun clearLog() {
        _diagnosticLog.value = emptyList()
    }

    /**
     * Connect to TV via wss://IP:3001 with SSL (matching test-lg.js).
     */
    suspend fun connectAndRegister(ip: String, clientKey: String? = null): String? {
        disconnect()
        clearLog()
        _connectionState.value = ConnectionState.CONNECTING

        log("PASO 1", "Abriendo WebSocket SSL a wss://$ip:3001")

        return try {
            val url = "wss://$ip:3001"
            val request = Request.Builder().url(url).build()

            val connected = CompletableDeferred<Boolean>()
            val registerResponse = CompletableDeferred<SsapMessage?>()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    log("PASO 2", "✅ WebSocket SSL CONECTADO a $ip:3001")
                    log("PASO 3", "Enviando register con manifest (test-lg.js)...")
                    connected.complete(true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val msg = SsapMessage.fromJson(text)
                    if (msg == null) {
                        logError("PARSE", "No se pudo parsear la respuesta JSON")
                        return
                    }

                    log("PASO 4", "Respuesta: type=${msg.type}, id=${msg.id}")

                    // La TV responde con "registered" (no "register") al pairing exitoso
                    val isRegisterResponse = msg.type == "register" ||
                        msg.type == "registered" ||
                        (msg.id in pendingRequests)

                    if (isRegisterResponse) {
                        val key = msg.payload?.get("client-key")?.asString
                        val pairingType = msg.payload?.get("pairingType")?.asString
                        val returnValue = msg.payload?.get("returnValue")?.asBoolean
                        val errorMsg = msg.payload?.get("error")?.asString
                            ?: msg.payload?.get("errorMessage")?.asString

                        if (key != null) {
                            log("PASO 6", "✅ Client-key recibido: ${maskKey(key)}")
                            log("ÉXITO", "Pairing completado. type=${msg.type}")
                            _connectionState.value = ConnectionState.CONNECTED
                            _pairingEvent.tryEmit(PairingEvent.Success(key))
                            registerResponse.complete(msg)
                        } else if (pairingType == "PROMPT") {
                            log("PASO 5", "Pairing PROMPT solicitado — la TV debería mostrar un popup ahora")
                            _connectionState.value = ConnectionState.WAITING_USER_PROMPT
                            _pairingEvent.tryEmit(PairingEvent.WaitingForUser)
                        } else if (errorMsg != null) {
                            logError("RECHAZO", "La TV respondió con error: $errorMsg")
                            _connectionState.value = ConnectionState.ERROR
                            _pairingEvent.tryEmit(PairingEvent.Error("TV error: $errorMsg"))
                            registerResponse.complete(null)
                        } else if (returnValue == false) {
                            logError("RECHAZO", "returnValue=false — la TV rechazó el registro")
                            _connectionState.value = ConnectionState.ERROR
                            registerResponse.complete(null)
                        } else {
                            log("INDEFINIDO", "type=${msg.type}, returnValue=${returnValue}")
                        }
                    } else if (msg.type != "response") {
                        log("OTRO", "Mensaje tipo=${msg.type}")
                    }

                    pendingRequests.remove(msg.id)?.complete(msg)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    logError("WEBSOCKET", "FALLO: ${t.javaClass.simpleName}: ${t.message}")
                    if (response != null) {
                        logError("HTTP", "Response code: ${response.code}, message: ${response.message}")
                    }
                    _connectionState.value = ConnectionState.ERROR
                    _pairingEvent.tryEmit(PairingEvent.Error(t.message ?: "Connection failed"))
                    connected.complete(false)
                    registerResponse.complete(null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    log("CLOSE", "WebSocket cerrado: code=$code, reason=$reason")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            })

            // Wait for connection
            val connectResult = withTimeoutOrNull(10_000L) { connected.await() }
            if (connectResult != true) {
                logError("PASO 2", "❌ TIMEOUT esperando conexión WebSocket SSL (10s)")
                return null
            }

            // Send register message (matches test-lg.js payload)
            _connectionState.value = ConnectionState.REGISTERING
            val registerMsg = SsapMessage.register(clientKey)
            val registerJson = registerMsg.toJson()
            log("PASO 3", "Register enviado")
            webSocket?.send(registerJson)

            // Wait for register response
            log("ESPERA", "Esperando respuesta de la TV (15s timeout)...")
            val result = withTimeoutOrNull(15_000L) {
                val response = registerResponse.await()
                response?.payload?.get("client-key")?.asString
            }

            if (result == null) {
                logError("TIMEOUT", "❌ No se recibió respuesta de la TV en 15 segundos")
                log("DIAGNÓSTICO", "Verificá: 1) LG Connect Apps activo, 2) Puerto 3001 abierto, 3) TV encendida, 4) Misma red")
            }

            result
        } catch (e: Exception) {
            logError("EXCEPCIÓN", "${e.javaClass.simpleName}: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
            null
        }
    }

    suspend fun request(uri: String, payload: com.google.gson.JsonObject = com.google.gson.JsonObject()): SsapMessage? {
        val ws = webSocket
        if (ws == null) {
            logError("REQUEST", "WebSocket es null — no hay conexión activa")
            return null
        }

        val msg = SsapMessage.request(uri, payload)
        val json = msg.toJson()
        log("REQUEST", "Enviando: uri=$uri")

        val deferred = CompletableDeferred<SsapMessage?>()
        pendingRequests[msg.id] = deferred
        val sent = ws.send(json)
        if (!sent) {
            logError("REQUEST", "❌ WebSocket.send() retornó false — socket puede estar cerrado")
            pendingRequests.remove(msg.id)
            return null
        }
        log("REQUEST", "Mensaje enviado OK, esperando respuesta (10s timeout)...")

        val response = withTimeoutOrNull(10_000L) { deferred.await() }
        if (response == null) {
            logError("REQUEST", "❌ TIMEOUT o respuesta nula para uri=$uri")
        } else {
            log("REQUEST", "✅ Respuesta recibida para uri=$uri")
        }
        return response
    }

    fun disconnect() {
        pendingRequests.values.forEach { it.complete(null) }
        pendingRequests.clear()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    private companion object {
        const val TAG = "SsapClient"

        /**
         * Create OkHttpClient with SSL that accepts self-signed certificates.
         * Equivalent to Node.js: { rejectUnauthorized: false }
         */
        fun createSslClient(): OkHttpClient {
            // Trust all certificates — equivalent to rejectUnauthorized: false
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        }

        suspend fun <T> withTimeoutOrNull(timeoutMs: Long, block: suspend () -> T): T? {
            return try {
                kotlinx.coroutines.withTimeout(timeoutMs) { block() }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                null
            }
        }

        fun maskKey(key: String): String {
            if (key.length < 12) return "****"
            return "${key.take(6)}...${key.takeLast(4)}"
        }
    }
}
