package com.kastlg.app.data.tv.discovery

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.Socket

/**
 * Discovers LG webOS TVs on the local network using SSDP (Simple Service Discovery Protocol).
 * Sends M-SEARCH multicast and parses responses to find TVs.
 */
class TvDiscoveryManager {
    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    companion object {
        private const val TAG = "TvDiscovery"
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SEARCH_TIMEOUT_MS = 5000
        private const val CONNECT_TIMEOUT_MS = 2000

        // M-SEARCH request for UPnP devices
        private val M_SEARCH = (
            "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 3\r\n" +
            "ST: urn:schemas-upnp-org:device:Basic:1\r\n" +
            "\r\n"
            ).toByteArray()
    }

    /**
     * Scan the local network for LG webOS TVs.
     * Returns a list of discovered TVs.
     */
    suspend fun scan(): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _discoveredTvs.value = emptyList()

        try {
            val tvs = mutableListOf<DiscoveredTv>()
            val foundIps = mutableSetOf<String>()

            // Method 1: SSDP multicast
            Log.d(TAG, "Starting SSDP discovery...")
            val ssdpTvs = discoverViaSsdp()
            for (tv in ssdpTvs) {
                if (tv.ip !in foundIps) {
                    foundIps.add(tv.ip)
                    tvs.add(tv)
                    _discoveredTvs.value = tvs.toList()
                }
            }

            // Method 2: Scan common IP ranges for port 3001/3000
            if (tvs.isEmpty()) {
                Log.d(TAG, "SSDP found nothing, scanning local subnet...")
                val scannedTvs = scanLocalSubnet()
                for (tv in scannedTvs) {
                    if (tv.ip !in foundIps) {
                        foundIps.add(tv.ip)
                        tvs.add(tv)
                        _discoveredTvs.value = tvs.toList()
                    }
                }
            }

            Log.d(TAG, "Discovery complete: ${tvs.size} TVs found")
            tvs
        } catch (e: Exception) {
            Log.e(TAG, "Discovery failed: ${e.message}")
            emptyList()
        } finally {
            _isScanning.value = false
        }
    }

    private fun discoverViaSsdp(): List<DiscoveredTv> {
        val tvs = mutableListOf<DiscoveredTv>()

        try {
            val socket = DatagramSocket()
            socket.soTimeout = SEARCH_TIMEOUT_MS

            val group = InetAddress.getByName(SSDP_ADDRESS)
            val packet = DatagramPacket(M_SEARCH, M_SEARCH.size, group, SSDP_PORT)
            socket.send(packet)

            // Receive responses
            val buffer = ByteArray(4096)
            val deadline = System.currentTimeMillis() + SEARCH_TIMEOUT_MS

            while (System.currentTimeMillis() < deadline) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    val response = String(responsePacket.data, 0, responsePacket.length)

                    // Extract IP from LOCATION header
                    val location = extractHeader(response, "LOCATION")
                    if (location != null) {
                        val ip = extractIpFromUrl(location)
                        if (ip != null && isValidTvResponse(response)) {
                            val name = extractTvName(response, location)
                            tvs.add(
                                DiscoveredTv(
                                    ip = ip,
                                    name = name,
                                    modelName = extractModelName(response),
                                )
                            )
                            Log.d(TAG, "Found TV via SSDP: $ip ($name)")
                        }
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    break
                }
            }

            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "SSDP error: ${e.message}")
        }

        return tvs
    }

    private fun scanLocalSubnet(): List<DiscoveredTv> {
        val tvs = mutableListOf<DiscoveredTv>()

        try {
            // Get local IP to determine subnet
            val localIp = getLocalIp() ?: return emptyList()
            val subnet = localIp.substringBeforeLast(".")

            // Scan common IPs in the subnet (skip .0 and .255)
            for (i in 1..254) {
                val ip = "$subnet.$i"
                if (ip == localIp) continue

                // Quick check: can we connect to port 3001 (SSL) or 3000?
                if (isPortOpen(ip, 3001, CONNECT_TIMEOUT_MS) ||
                    isPortOpen(ip, 3000, CONNECT_TIMEOUT_MS)
                ) {
                    val port = if (isPortOpen(ip, 3001, CONNECT_TIMEOUT_MS)) 3001 else 3000
                    val ssl = port == 3001
                    tvs.add(
                        DiscoveredTv(
                            ip = ip,
                            name = "TV en $ip",
                            port = port,
                            isSsl = ssl,
                        )
                    )
                    Log.d(TAG, "Found TV via subnet scan: $ip:$port")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Subnet scan error: ${e.message}")
        }

        return tvs
    }

    private fun isValidTvResponse(response: String): Boolean {
        // Check if response looks like an LG webOS TV
        val server = extractHeader(response, "SERVER")?.lowercase() ?: ""
        val st = extractHeader(response, "ST")?.lowercase() ?: ""
        return server.contains("webos") ||
            server.contains("lg") ||
            st.contains("webos") ||
            st.contains("lg")
    }

    private fun extractTvName(response: String, location: String): String {
        val friendlyName = extractHeader(response, "friendlyName")
        if (!friendlyName.isNullOrBlank()) return friendlyName

        val modelName = extractModelName(response)
        if (modelName.isNotBlank()) return "LG $modelName"

        // Try to get name from location URL path
        val path = location.substringAfterLast("/").substringBefore("?")
        if (path.isNotBlank() && path != "description.xml") return path

        return "LG webOS TV"
    }

    private fun extractModelName(response: String): String {
        return extractHeader(response, "modelName")
            ?: extractHeader(response, "model-name")
            ?: ""
    }

    private fun extractHeader(response: String, header: String): String? {
        val lines = response.split("\r\n", "\n")
        for (line in lines) {
            if (line.startsWith("$header:", ignoreCase = true)) {
                return line.substringAfter(":").trim()
            }
        }
        return null
    }

    private fun extractIpFromUrl(url: String): String? {
        // URL format: http://192.168.1.100:1900/description.xml
        return try {
            val host = java.net.URL(url).host
            if (host.isNotBlank()) host else null
        } catch (_: Exception) {
            // Try regex for IP in URL
            val regex = Regex("""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})""")
            regex.find(url)?.groupValues?.get(1)
        }
    }

    private fun getLocalIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP: ${e.message}")
        }
        return null
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
