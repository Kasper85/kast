package com.kastlg.app.data.remote.flixcorn

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure URL-construction tests (REQ-SEARCH-01 / REQ-SEARCH-02).
 *
 * These assert that every Flixcorn endpoint URL is RFC 3986 percent-encoded
 * (OkHttp [okhttp3.HttpUrl]) so no raw space, accent, or reserved character can
 * ever reach `Request.Builder.url(...)` — the `IllegalArgumentException` that
 * previously surfaced as a false `FlixcornError.UNREACHABLE`.
 */
class FlixcornScraperUrlTest {

    private val scraper = FlixcornScraper()

    @Test
    fun `search query with spaces is percent encoded as Bestias per cent 20 Divinas`() {
        val url = scraper.searchUrl("Bestias Divinas")

        assertEquals("https://www.flixcorn.net/search?q=Bestias%20Divinas", url)
    }

    @Test
    fun `search query with accents is utf8 percent encoded`() {
        val url = scraper.searchUrl("Café")

        assertEquals("https://www.flixcorn.net/search?q=Caf%C3%A9", url)
    }

    @Test
    fun `single word safe query keeps current encoding with no double encode`() {
        val url = scraper.searchUrl("bestias")

        assertEquals("https://www.flixcorn.net/search?q=bestias", url)
    }

    @Test
    fun `episode path percent encodes slug with space in the path segment`() {
        val url = scraper.episodeUrl("mi serie", 2, 3)

        assertEquals(
            "https://www.flixcorn.net/ver/mi%20serie/temporada-2/capitulo-3.html",
            url,
        )
    }

    @Test
    fun `external endpoint keeps token path and s equals 1 query`() {
        val url = scraper.externalUrl("tok-abc")

        assertEquals("https://www.flixcorn.net/external/tok-abc?s=1", url)
    }

    @Test
    fun `plus sign in query is percent encoded as percent 2B`() {
        val url = scraper.searchUrl("bad+words")

        assertEquals("https://www.flixcorn.net/search?q=bad%2Bwords", url)
    }
}
