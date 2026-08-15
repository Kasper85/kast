package com.kastlg.app.data.remote.flixcorn

import org.junit.Assert.assertEquals
import org.junit.Test

class FlixcornHtmlParserTest {

    @Test
    fun parseEpisodeServers_extractsOnlineAndDirectServers() {
        val sampleHtml = """
            <div class="cap-quality-section">
                <span class="cap-qual-badge">1080p HD</span>
                <div class="cap-lang-node">
                    <span class="cap-lang-name">Español Latino</span>
                    <div class="cap-server-row">
                        <span class="cap-server-name">Voe</span>
                        <div class="cap-server-actions">
                            <a href="/player/9a7dfe1718" class="cap-btn cap-btn--online">VER ONLINE</a>
                            <a href="/external/9a7dfe1718" class="cap-btn cap-btn--direct">LINK DIRECTO</a>
                        </div>
                    </div>
                    <div class="cap-server-row">
                        <span class="cap-server-name">1fichier</span>
                        <div class="cap-server-actions">
                            <a href="/external/abc12345" class="cap-btn cap-btn--direct">LINK DIRECTO</a>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val servers = FlixcornHtmlParser.parseEpisodeServers(sampleHtml)

        // Both Voe (online + direct) and 1fichier (direct-only) are extracted.
        assertEquals(2, servers.size)

        // sortServersByPriority puts Voe first.
        val voe = servers.first()
        assertEquals("Voe", voe.serverName)
        assertEquals("1080p HD", voe.quality)
        assertEquals("Español Latino", voe.language)
        assertEquals("https://www.flixcorn.net/player/9a7dfe1718", voe.onlineUrl)
        assertEquals("https://www.flixcorn.net/external/9a7dfe1718", voe.directUrl)

        val oneFichier = servers[1]
        assertEquals("1fichier", oneFichier.serverName)
        assertEquals("Español Latino", oneFichier.language)
        assertEquals(null, oneFichier.onlineUrl)
        assertEquals("https://www.flixcorn.net/external/abc12345", oneFichier.directUrl)
    }
}
