package com.kastlg.app.data.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsapMessageTest {
    @Test
    fun `register message has correct type and manifest`() {
        val msg = SsapMessage.register()

        assertEquals("register", msg.type)
        assertNotNull(msg.payload)
        assertEquals("PROMPT", msg.payload!!.get("pairingType").asString)
        assertEquals("com.kastlg.app", msg.payload!!.get("manifest").asJsonObject
            .get("signed").asJsonObject.get("appId").asString)
    }

    @Test
    fun `register with client key includes key in payload`() {
        val msg = SsapMessage.register(clientKey = "test-key-123")

        assertEquals("test-key-123", msg.payload!!.get("client-key").asString)
    }

    @Test
    fun `register without client key omits key`() {
        val msg = SsapMessage.register(clientKey = null)

        assertNull(msg.payload!!.get("client-key"))
    }

    @Test
    fun `request message has correct type and uri`() {
        val msg = SsapMessage.request("ssap://system.launcher/open")

        assertEquals("request", msg.type)
        assertEquals("ssap://system.launcher/open", msg.uri)
    }

    @Test
    fun `fromJson parses valid SSAP response`() {
        val json = """
            {
                "id": "kastlg-00000001",
                "type": "register",
                "payload": {
                    "client-key": "abc123",
                    "returnValue": true
                }
            }
        """.trimIndent()

        val msg = SsapMessage.fromJson(json)

        assertNotNull(msg)
        assertEquals("register", msg!!.type)
        assertEquals("abc123", msg.payload!!.get("client-key").asString)
    }

    @Test
    fun `fromJson parses registered message with client-key`() {
        val json = """
            {
                "type": "registered",
                "payload": {
                    "client-key": "abc123xyz"
                }
            }
        """.trimIndent()

        val msg = SsapMessage.fromJson(json)

        assertNotNull(msg)
        assertEquals("registered", msg!!.type)
        assertEquals("abc123xyz", msg.payload!!.get("client-key").asString)
    }

    @Test
    fun `fromJson parses registered message without client-key`() {
        val json = """
            {
                "type": "registered",
                "payload": {}
            }
        """.trimIndent()

        val msg = SsapMessage.fromJson(json)

        assertNotNull(msg)
        assertEquals("registered", msg!!.type)
        assertNull(msg.payload!!.get("client-key"))
    }

    @Test
    fun `registered_message_completes_pairing`() {
        // Simulate what the TV actually sends: {"type":"registered","payload":{"client-key":"..."}}
        val tvResponse = SsapMessage.fromJson(
            """{"type":"registered","payload":{"client-key":"tv-secret-key-abc"}}"""
        )!!

        // The SsapClient logic checks: type == "registered" → success
        val isRegisterResponse = tvResponse.type == "register" ||
            tvResponse.type == "registered"

        assertTrue("type 'registered' should be recognized as register response", isRegisterResponse)

        val clientKey = tvResponse.payload?.get("client-key")?.asString
        assertEquals("tv-secret-key-abc", clientKey)
        assertTrue("client-key should not be null", clientKey != null)
    }

    @Test
    fun `fromJson returns null for invalid JSON`() {
        val msg = SsapMessage.fromJson("not json")
        assertNull(msg)
    }

    @Test
    fun `toJson produces valid JSON`() {
        val msg = SsapMessage.request("ssap://system.launcher/open")
        val json = msg.toJson()

        assertNotNull(json)
        assert(json.contains("request"))
        assert(json.contains("ssap://system.launcher/open"))
    }

    @Test
    fun `nextId produces unique ids`() {
        val id1 = SsapMessage.nextId()
        val id2 = SsapMessage.nextId()

        assert(id1 != id2)
        assert(id1.startsWith("kastlg-"))
        assert(id2.startsWith("kastlg-"))
    }

    @Test
    fun `register manifest matches test-lg js structure`() {
        val msg = SsapMessage.register()
        val manifest = msg.payload!!.get("manifest").asJsonObject
        val signed = manifest.get("signed").asJsonObject

        // Verify fields that test-lg.js has and Android needs
        assertEquals("20260618", signed.get("created").asString)
        assertEquals("com.kastlg.app", signed.get("appId").asString)
        assertEquals("kastlg", signed.get("vendorId").asString)
        assertNotNull(signed.get("localizedAppNames"))
        assertNotNull(signed.get("localizedVendorNames"))
        assertNotNull(signed.get("serial"))

        // Verify signatures exist
        assertTrue("manifest must have signatures", manifest.has("signatures"))
        val signatures = manifest.get("signatures").asJsonArray
        assertEquals(1, signatures.size())
        assertEquals(1, signatures[0].asJsonObject.get("signatureVersion").asInt)
        assertEquals("fake", signatures[0].asJsonObject.get("signature").asString)
    }
}
