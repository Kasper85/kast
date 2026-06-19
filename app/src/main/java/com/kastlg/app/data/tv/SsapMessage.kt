package com.kastlg.app.data.tv

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * SSAP (Smart Service Access Protocol) message for LG webOS TV communication.
 * Protocol: JSON over WebSocket on port 3001 (SSL) or 3000 (plain).
 *
 * Manifest structure matches the working test-lg.js handshake exactly.
 */
data class SsapMessage(
    val id: String,
    val type: String,
    val uri: String? = null,
    val payload: JsonObject? = null,
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        private var counter = 0L

        fun nextId(): String {
            counter++
            return "kastlg-${String.format("%08x", counter)}"
        }

        fun fromJson(json: String): SsapMessage? = try {
            val obj = JsonParser.parseString(json).asJsonObject
            SsapMessage(
                id = obj.get("id")?.asString ?: "",
                type = obj.get("type")?.asString ?: "",
                uri = obj.get("uri")?.asString,
                payload = obj.get("payload")?.asJsonObject,
            )
        } catch (_: Exception) {
            null
        }

        /**
         * Create a register message matching test-lg.js exactly.
         * @param clientKey Existing client key for reconnection, null for first-time pairing.
         */
        fun register(clientKey: String? = null): SsapMessage {
            val payload = buildRegisterPayload(clientKey)
            return SsapMessage(
                id = nextId(),
                type = "register",
                payload = payload,
            )
        }

        fun request(uri: String, payload: JsonObject = JsonObject()): SsapMessage = SsapMessage(
            id = nextId(),
            type = "request",
            uri = uri,
            payload = payload,
        )

        /**
         * Build register payload matching test-lg.js exactly.
         * This is the payload that works with the real LG webOS TV.
         */
        private fun buildRegisterPayload(clientKey: String? = null): JsonObject {
            val root = JsonObject()
            root.addProperty("forcePairing", false)
            root.addProperty("pairingType", "PROMPT")

            if (clientKey != null) {
                root.addProperty("client-key", clientKey)
            }

            // Manifest — matches test-lg.js exactly
            val manifest = JsonObject()
            manifest.addProperty("manifestVersion", 1)
            manifest.addProperty("appVersion", "1.0")

            // signed block — matches test-lg.js exactly
            val signed = JsonObject()
            signed.addProperty("created", "20260618")
            signed.addProperty("appId", "com.kastlg.app")
            signed.addProperty("vendorId", "kastlg")

            val localizedAppNames = JsonObject()
            localizedAppNames.addProperty("", "KastLG")
            signed.add("localizedAppNames", localizedAppNames)

            val localizedVendorNames = JsonObject()
            localizedVendorNames.addProperty("", "KastLG")
            signed.add("localizedVendorNames", localizedVendorNames)

            val signedPermissions = JsonArray()
            listOf(
                "LAUNCH",
                "LAUNCH_WEBAPP",
                "APP_TO_APP",
                "CONTROL_INPUT_MEDIA_PLAYBACK",
                "CONTROL_POWER",
                "READ_INSTALLED_APPS",
            ).forEach { signedPermissions.add(it) }
            signed.add("permissions", signedPermissions)

            signed.addProperty("serial", "2f930e2d2cfe083771f68e4fe7bb07")

            manifest.add("signed", signed)

            // permissions — matches test-lg.js exactly (same as signed.permissions)
            val permissions = JsonArray()
            listOf(
                "LAUNCH",
                "LAUNCH_WEBAPP",
                "APP_TO_APP",
                "CONTROL_INPUT_MEDIA_PLAYBACK",
                "CONTROL_POWER",
                "READ_INSTALLED_APPS",
            ).forEach { permissions.add(it) }
            manifest.add("permissions", permissions)

            // signatures — matches test-lg.js exactly
            val signatures = JsonArray()
            val signature = JsonObject()
            signature.addProperty("signatureVersion", 1)
            signature.addProperty("signature", "fake")
            signatures.add(signature)
            manifest.add("signatures", signatures)

            root.add("manifest", manifest)
            return root
        }
    }
}
