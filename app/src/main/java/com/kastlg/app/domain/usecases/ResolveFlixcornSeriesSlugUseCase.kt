package com.kastlg.app.domain.usecases

import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.domain.repositories.FlixcornRepository
import java.text.Normalizer
import java.util.Locale

/**
 * Resolves the Flixcorn slug for a TMDB series title so the user can be
 * navigated to the matching Flixcorn episode page.
 *
 * The search query is matched against result titles with accent- and
 * punctuation-insensitive normalization. An exact normalized match wins;
 * otherwise the first search result is used as a best-effort fallback.
 */
class ResolveFlixcornSeriesSlugUseCase(
    private val repository: FlixcornRepository,
) {

    suspend operator fun invoke(title: String): FlixcornResult<String?> {
        return when (val result = repository.searchSeries(title)) {
            is FlixcornResult.Success -> {
                val normalizedTitle = normalize(title)
                val slug = result.data
                    .firstOrNull { normalize(it.title) == normalizedTitle }
                    ?.slug
                    ?: result.data.firstOrNull()?.slug
                FlixcornResult.Success(slug)
            }
            is FlixcornResult.Error -> result
            is FlixcornResult.Loading -> result
        }
    }

    companion object {
        /**
         * Lowercases the value and strips diacritics and punctuation so that
         * titles like "Béastars!" and "Beastars" compare as equal.
         */
        fun normalize(value: String): String {
            val withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
            return withoutDiacritics
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
