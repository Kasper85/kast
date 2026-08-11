package com.kastlg.app.data.remote.flixcorn

import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class FlixcornScraper(
    private val parser: FlixcornHtmlParser = FlixcornHtmlParser,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Volatile
    private var lastRequestTime = 0L

    private suspend fun rateLimit() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            delay(MIN_REQUEST_INTERVAL_MS - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    suspend fun searchSeries(query: String): FlixcornResult<List<FlixcornSearchResult>> {
        if (query.length < MIN_QUERY_LENGTH || query.length > MAX_QUERY_LENGTH) {
            return FlixcornResult.Error(FlixcornError.NETWORK_TIMEOUT)
        }

        return executeWithRetry {
            rateLimit()
            val url = "$BASE_URL/search?q=${query.trim()}"
            val html = fetchHtml(url)
            val results = parser.parseSearchResults(html)
            Log.i(TAG, "searchSeries query=$query results=${results.size}")
            FlixcornResult.Success(results)
        }
    }

    suspend fun getSeriesDetail(slug: String): FlixcornResult<FlixcornSeriesDetail> {
        return executeWithRetry {
            rateLimit()
            val url = "$BASE_URL/serie/$slug.html"
            val html = fetchHtml(url)
            val detail = parser.parseSeriesDetail(html, slug)
            if (detail != null) {
                Log.i(TAG, "getSeriesDetail slug=$slug title=${detail.title}")
                FlixcornResult.Success(detail)
            } else {
                Log.w(TAG, "getSeriesDetail slug=$slug parse returned null")
                FlixcornResult.Error(FlixcornError.PARSE_FAILURE)
            }
        }
    }

    suspend fun getEpisodeServers(
        slug: String,
        season: Int,
        episode: Int,
    ): FlixcornResult<List<StreamingServer>> {
        return executeWithRetry {
            rateLimit()
            val url = "$BASE_URL/ver/$slug/temporada-$season/capitulo-$episode.html"
            val html = fetchHtml(url)
            val servers = parser.parseEpisodeServers(html)
            Log.i(TAG, "getEpisodeServers slug=$slug s${season}e$episode servers=${servers.size}")
            if (servers.isNotEmpty()) {
                FlixcornResult.Success(servers)
            } else {
                FlixcornResult.Error(FlixcornError.NO_SERVERS_FOUND)
            }
        }
    }

    suspend fun resolvePlayerUrl(token: String): FlixcornResult<String> {
        return executeWithRetry {
            rateLimit()
            val url = "$BASE_URL/player/$token"
            val html = fetchHtml(url)
            val resolvedToken = parser.parsePlayerToken(html)
            if (resolvedToken != null) {
                val videoUrl = "$BASE_URL/external/$resolvedToken?s=1"
                Log.i(TAG, "resolvePlayerUrl token=$token resolved=$resolvedToken")
                FlixcornResult.Success(videoUrl)
            } else {
                val fallbackUrl = "$BASE_URL/external/$token?s=1"
                Log.w(TAG, "resolvePlayerUrl token=$token using fallback direct link")
                FlixcornResult.Success(fallbackUrl)
            }
        }
    }

    private suspend fun <T> executeWithRetry(block: suspend () -> FlixcornResult<T>): FlixcornResult<T> {
        var lastError: FlixcornError? = null
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                Log.w(TAG, "Network error attempt=${attempt + 1}: ${e.message}")
                lastError = FlixcornError.NETWORK_TIMEOUT
                if (attempt < MAX_RETRIES) {
                    delay(RETRY_BACKOFF_MS * (attempt + 1))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                return FlixcornResult.Error(FlixcornError.UNREACHABLE)
            }
        }
        return FlixcornResult.Error(lastError ?: FlixcornError.NETWORK_TIMEOUT)
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty response body")

        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message}")
        }

        return body
    }

    companion object {
        private const val TAG = "FlixcornScraper"
        private const val BASE_URL = "https://www.flixcorn.net"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
        private const val MIN_REQUEST_INTERVAL_MS = 1000L
        private const val MIN_QUERY_LENGTH = 2
        private const val MAX_QUERY_LENGTH = 100
        private const val MAX_RETRIES = 2
        private const val RETRY_BACKOFF_MS = 1500L
    }
}
