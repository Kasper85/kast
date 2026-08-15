package com.kastlg.app.data.remote.flixcorn

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object FlixcornHtmlParser {
    private const val TAG = "FlixcornParser"

    private const val SELECTOR_SEARCH_CARD = ".media-card"
    private const val SELECTOR_SERIES_TITLE = "h1.fw-bold"
    private const val SELECTOR_SERIES_POSTER = ".serie-poster"
    private const val SELECTOR_SERIES_BACKDROP = ".serie-backdrop"
    private const val SELECTOR_GENRE_LINK = ".genre-link"
    private const val SELECTOR_EPISODE_ROW = ".ep-row"
    private const val SELECTOR_EPISODE_BADGE = ".ep-row-badge"
    private const val SELECTOR_EPISODE_TITLE = ".ep-row-title"
    private const val SELECTOR_EPISODE_SYNOPSIS = ".ep-row-synopsis"
    private const val SELECTOR_QUALITY_SECTION = ".cap-quality-section"
    private const val SELECTOR_QUALITY_BADGE = ".cap-qual-badge"
    private const val SELECTOR_LANG_NODE = ".cap-lang-node"
    private const val SELECTOR_LANG_NAME = ".cap-lang-name"
    private const val SELECTOR_SERVER_ROW = ".cap-server-row"
    private const val SELECTOR_SERVER_NAME = ".cap-server-name"
    private const val SELECTOR_SERVER_ICON = ".cap-server-icon"
    private const val SELECTOR_BTN_ONLINE = ".cap-btn--online"
    private const val SELECTOR_BTN_DIRECT = ".cap-btn--direct"
    private const val SELECTOR_PLAYER_META = "#plyr-meta"
    private const val ATTR_DATA_LINK_TOKEN = "data-link-token"
    private const val BASE_URL = "https://www.flixcorn.net"

    fun parseSearchResults(html: String): List<FlixcornSearchResult> {
        val doc = Jsoup.parse(html)
        val cards = doc.select(SELECTOR_SEARCH_CARD)

        return cards.mapNotNull { card ->
            parseSearchCard(card)
        }
    }

    private fun parseSearchCard(card: org.jsoup.nodes.Element): FlixcornSearchResult? {
        val title = card.selectFirst(".media-title")?.text()?.trim() ?: return null
        val href = card.attr("href")
        val slug = href
            .removePrefix("/serie/")
            .removeSuffix(".html")
            .trim()
        if (slug.isBlank()) return null

        val metaSpans = card.select(".media-meta span")
        val year = metaSpans.firstOrNull()?.text()?.trim()?.toIntOrNull()

        val genreText = metaSpans.drop(1).joinToString(", ") { it.text().trim() }
        val genres = genreText
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "\u00b7" }

        val posterUrl = card.selectFirst("img")?.attr("src")?.let { buildAbsoluteUrl(it) }

        return FlixcornSearchResult(
            title = title,
            slug = slug,
            year = year,
            posterUrl = posterUrl,
            genres = genres,
        )
    }

    fun parseSeriesDetail(html: String, slug: String): FlixcornSeriesDetail? {
        val doc = Jsoup.parse(html)

        val title = doc.selectFirst(SELECTOR_SERIES_TITLE)?.text()?.trim() ?: return null

        val posterUrl = doc.selectFirst(SELECTOR_SERIES_POSTER)
            ?.attr("src")
            ?.let { buildAbsoluteUrl(it) }

        val backdropStyle = doc.selectFirst(SELECTOR_SERIES_BACKDROP)
            ?.attr("style") ?: ""
        val backdropUrl = extractBackgroundImage(backdropStyle)

        val overview = doc.selectFirst(".glass")?.text()?.trim() ?: ""

        val metaItems = doc.select(".serie-meta-item")
        var year: Int? = null
        var numberOfSeasons = 0
        var rating: Double? = null
        var status = ""

        metaItems.forEach { item ->
            val text = item.text().trim()
            when {
                item.selectFirst("i.bi-calendar3") != null -> {
                    year = text.replace("\\D".toRegex(), "").toIntOrNull()
                }
                item.selectFirst("i.bi-collection-fill") != null -> {
                    numberOfSeasons = text.replace("\\D".toRegex(), "").toIntOrNull() ?: 0
                }
                item.selectFirst("i.bi-star-fill") != null -> {
                    rating = text.replace(",", ".").toDoubleOrNull()
                }
                item.selectFirst("i.bi-circle-fill") != null -> {
                    status = text.trim()
                }
            }
        }

        val genres = doc.select(SELECTOR_GENRE_LINK).map { it.text().trim() }

        val episodeRows = doc.select(SELECTOR_EPISODE_ROW)
        val episodes = episodeRows.mapNotNull { row -> parseEpisodeRow(row) }

        val groupedEpisodes = episodes.groupBy { it.seasonNumber }
            .toSortedMap()
            .map { (seasonNum, seasonEps) ->
                FlixcornSeason(
                    seasonNumber = seasonNum,
                    episodes = seasonEps.sortedBy { it.episodeNumber },
                )
            }

        val calculatedSeasons = if (numberOfSeasons > 0) numberOfSeasons
        else groupedEpisodes.size.coerceAtLeast(1)

        return FlixcornSeriesDetail(
            slug = slug,
            title = title,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            overview = overview,
            year = year,
            rating = rating,
            genres = genres,
            numberOfSeasons = calculatedSeasons,
            numberOfEpisodes = episodes.size,
            status = status,
            seasons = groupedEpisodes,
        )
    }

    private fun parseEpisodeRow(row: org.jsoup.nodes.Element): FlixcornEpisode? {
        val badge = row.selectFirst(SELECTOR_EPISODE_BADGE)?.text()?.trim() ?: return null
        val title = row.selectFirst(SELECTOR_EPISODE_TITLE)?.text()?.trim() ?: return null
        val synopsis = row.selectFirst(SELECTOR_EPISODE_SYNOPSIS)?.text()?.trim() ?: ""

        val href = row.attr("href")
        val seasonMatch = Regex("temporada-(\\d+)").find(href)
        val episodeMatch = Regex("capitulo-(\\d+)").find(href)

        val seasonNumber = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val episodeNumber = episodeMatch?.groupValues?.get(1)?.toIntOrNull()
            ?: badge.replace("\\D".toRegex(), "").toIntOrNull()
            ?: 1

        return FlixcornEpisode(
            episodeNumber = episodeNumber,
            title = title,
            synopsis = synopsis,
            seasonNumber = seasonNumber,
        )
    }

    fun parseEpisodeServers(html: String): List<StreamingServer> {
        val doc = Jsoup.parse(html)
        val servers = mutableListOf<StreamingServer>()

        val qualitySections = doc.select(SELECTOR_QUALITY_SECTION)

        qualitySections.forEach { section ->
            val quality = section.selectFirst(SELECTOR_QUALITY_BADGE)?.text()?.trim() ?: "HD"

            val langNodes = section.select(SELECTOR_LANG_NODE)

            langNodes.forEach { langNode ->
                val language = langNode.selectFirst(SELECTOR_LANG_NAME)?.text()?.trim() ?: "Unknown"

                val serverRows = langNode.select(SELECTOR_SERVER_ROW)

                serverRows.forEach { row ->
                    val server = parseServerRow(row, quality, language)
                    if (server != null) {
                        servers.add(server)
                    }
                }
            }
        }

        return servers.sortedOnlineFirst()
    }

    private fun parseServerRow(
        row: org.jsoup.nodes.Element,
        quality: String,
        language: String,
    ): StreamingServer? {
        val serverName = row.selectFirst(SELECTOR_SERVER_NAME)?.text()?.trim() ?: return null

        val onlineHref = row.selectFirst(SELECTOR_BTN_ONLINE)?.attr("href")
        val directHref = row.selectFirst(SELECTOR_BTN_DIRECT)?.attr("href")

        if (onlineHref == null && directHref == null) return null

        val iconUrl = row.selectFirst(SELECTOR_SERVER_ICON)
            ?.attr("src")
            ?.let { buildAbsoluteUrl(it) }

        return StreamingServer(
            serverName = serverName,
            quality = quality,
            language = language,
            onlineUrl = onlineHref?.let { buildAbsoluteUrl(it) },
            directUrl = directHref?.let { buildAbsoluteUrl(it) },
            serverIconUrl = iconUrl,
        )
    }

    fun parsePlayerToken(html: String): String? {
        val doc = Jsoup.parse(html)
        return doc.selectFirst(SELECTOR_PLAYER_META)
            ?.attr(ATTR_DATA_LINK_TOKEN)
            ?.takeIf { it.isNotBlank() }
    }

    private fun buildAbsoluteUrl(path: String): String {
        if (path.startsWith("http")) return path
        return "$BASE_URL${if (path.startsWith("/")) path else "/$path"}"
    }

    private fun extractBackgroundImage(style: String): String? {
        val match = Regex("url\\(['\"]?([^'\")]+)['\"]?\\)").find(style)
        return match?.groupValues?.get(1)?.let { buildAbsoluteUrl(it) }
    }
}
