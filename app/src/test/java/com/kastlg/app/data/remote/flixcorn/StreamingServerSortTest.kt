package com.kastlg.app.data.remote.flixcorn

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingServerSortTest {

    @Test
    fun `online server with player url ranks first`() {
        val servers = listOf(
            server(name = "Zeta", directOnly = true),
            server(name = "Alpha", online = true),
            server(name = "Beta", directOnly = true),
        )

        val sorted = servers.sortedOnlineFirst()

        assertEquals(3, sorted.size)
        assertEquals("Alpha", sorted[0].serverName)
        assertEquals("Beta", sorted[1].serverName)
        assertEquals("Zeta", sorted[2].serverName)
    }

    @Test
    fun `online servers rank before direct-only external servers`() {
        val servers = listOf(
            server(name = "1fichier", directOnly = true),
            server(name = "Voe", online = true),
        )

        val sorted = servers.sortedOnlineFirst()

        assertEquals("Voe", sorted[0].serverName)
        assertEquals("1fichier", sorted[1].serverName)
    }

    @Test
    fun `no online server falls back to name order`() {
        val servers = listOf(
            server(name = "Zeta", directOnly = true),
            server(name = "Alpha", directOnly = true),
        )

        val sorted = servers.sortedOnlineFirst()

        assertEquals(2, sorted.size)
        assertEquals("Alpha", sorted[0].serverName)
        assertEquals("Zeta", sorted[1].serverName)
        assertEquals(null, sorted[0].onlineUrl)
        assertEquals(null, sorted[1].onlineUrl)
    }

    @Test
    fun `tiebreak uses case insensitive server name`() {
        val servers = listOf(
            server(name = "beta", online = true),
            server(name = "Alpha", online = true),
        )

        val sorted = servers.sortedOnlineFirst()

        assertEquals("Alpha", sorted[0].serverName)
        assertEquals("beta", sorted[1].serverName)
    }

    @Test
    fun `empty list sorts to empty`() {
        val sorted = emptyList<StreamingServer>().sortedOnlineFirst()

        assertEquals(0, sorted.size)
    }

    private fun server(
        name: String,
        online: Boolean = false,
        directOnly: Boolean = false,
    ) = StreamingServer(
        serverName = name,
        quality = "1080p",
        language = "Español",
        onlineUrl = if (online) "https://www.flixcorn.net/player/abc123" else null,
        directUrl = if (directOnly) "https://www.flixcorn.net/external/abc123" else null,
        serverIconUrl = null,
    )
}
