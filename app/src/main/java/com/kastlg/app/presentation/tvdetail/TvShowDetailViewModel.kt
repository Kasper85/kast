package com.kastlg.app.presentation.tvdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.domain.models.Episode
import com.kastlg.app.domain.models.TvShowDetail
import com.kastlg.app.domain.repositories.FavoriteRepository
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import com.kastlg.app.domain.usecases.GetTvSeasonUseCase
import com.kastlg.app.domain.usecases.GetTvShowDetailUseCase
import com.kastlg.app.domain.usecases.ResolveFlixcornSeriesSlugUseCase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TvShowDetailViewModel(
    private val tvShowId: Int,
    private val getTvShowDetail: GetTvShowDetailUseCase,
    private val getTvSeason: GetTvSeasonUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val resolveFlixcornSeriesSlug: ResolveFlixcornSeriesSlugUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TvShowDetailUiState())
    val uiState: StateFlow<TvShowDetailUiState> = mutableUiState
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent
    private var loadedDetail: TvShowDetail? = null

    init {
        observeFavorite()
        loadDetail()
    }

    fun retry() {
        loadDetail()
    }

    fun toggleFavorite() {
        val detail = loadedDetail ?: return
        viewModelScope.launch {
            try {
                favoriteRepository.toggleTvShow(detail)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Favorite state is driven reactively from observeFavorite(); ignore persistence errors.
            }
        }
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            favoriteRepository.observeIsFavorite(tvShowId)
                .catch { error ->
                    if (error is CancellationException) throw error
                }
                .collect { isFavorite ->
                    mutableUiState.update { it.copy(isFavorite = isFavorite) }
                }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoadingSeason = true) }
            runCatching { getTvSeason(tvShowId, seasonNumber) }
                .onSuccess { season ->
                    mutableUiState.update {
                        it.copy(
                            selectedSeason = season,
                            isLoadingSeason = false,
                            selectedEpisode = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    mutableUiState.update { it.copy(isLoadingSeason = false) }
                }
        }
    }

    fun selectEpisode(episode: Episode) {
        mutableUiState.update { it.copy(selectedEpisode = episode, episodeNotice = null) }
        resolveEpisode(episode)
    }

    private fun resolveEpisode(episode: Episode) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isResolvingEpisode = true) }
            when (val result = resolveFlixcornSeriesSlug(seriesTitle())) {
                is FlixcornResult.Success -> {
                    val slug = result.data
                    if (slug != null) {
                        mutableUiState.update { it.copy(isResolvingEpisode = false) }
                        _navigationEvent.emit(
                            NavigationEvent.NavigateToFlixcornEpisode(
                                slug = slug,
                                season = episode.seasonNumber,
                                episode = episode.episodeNumber,
                            ),
                        )
                    } else {
                        mutableUiState.update {
                            it.copy(
                                isResolvingEpisode = false,
                                episodeNotice = "Este episodio no se encontró en Flixcorn.",
                            )
                        }
                    }
                }
                is FlixcornResult.Error -> {
                    mutableUiState.update {
                        it.copy(
                            isResolvingEpisode = false,
                            episodeNotice = "No se pudo buscar la serie en Flixcorn. Intenta de nuevo.",
                        )
                    }
                }
                is FlixcornResult.Loading -> Unit
            }
        }
    }

    private fun seriesTitle(): String = loadedDetail?.title ?: mutableUiState.value.title

    private fun loadDetail() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getTvShowDetail(tvShowId) }
                .onSuccess { detail ->
                    loadedDetail = detail
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            title = detail.title,
                            posterUrl = detail.posterUrl,
                            backdropUrl = detail.backdropUrl,
                            overview = detail.overview,
                            releaseYear = detail.releaseDate.take(4).ifBlank { "—" },
                            voteAverage = detail.voteAverage,
                            genres = detail.genres,
                            numberOfSeasons = detail.numberOfSeasons,
                            numberOfEpisodes = detail.numberOfEpisodes,
                            status = detail.status,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toActionableMessage(),
                        )
                    }
                }
        }
    }

    private fun Throwable.toActionableMessage(): String = when (this) {
        is MissingTmdbTokenException -> message.orEmpty()
        is IOException -> "No se pudo conectar con TMDB. Verifica tu conexión a internet."
        is HttpException -> when (code()) {
            401 -> "TMDB rechazó el token. Configura el token TMDB desde Ajustes."
            404 -> "Serie no encontrada en TMDB."
            429 -> "TMDB recibió demasiadas solicitudes. Espera un momento e intenta de nuevo."
            else -> "TMDB devolvió un error (${code()}). Intenta de nuevo en breve."
        }
        else -> "No se pudieron cargar los detalles. Intenta de nuevo."
    }

    sealed class NavigationEvent {
        data class NavigateToFlixcornEpisode(
            val slug: String,
            val season: Int,
            val episode: Int,
        ) : NavigationEvent()
    }
}
