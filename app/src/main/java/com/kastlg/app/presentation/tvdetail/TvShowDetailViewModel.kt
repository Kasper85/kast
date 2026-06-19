package com.kastlg.app.presentation.tvdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.tv.PlaybackUrlBuilder
import com.kastlg.app.domain.models.Episode
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import com.kastlg.app.domain.repositories.TvRepository
import com.kastlg.app.domain.usecases.GetTvSeasonUseCase
import com.kastlg.app.domain.usecases.GetTvShowDetailUseCase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TvShowDetailViewModel(
    private val tvShowId: Int,
    private val getTvShowDetail: GetTvShowDetailUseCase,
    private val getTvSeason: GetTvSeasonUseCase,
    private val tvRepository: TvRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TvShowDetailUiState())
    val uiState: StateFlow<TvShowDetailUiState> = mutableUiState

    init {
        loadDetail()
    }

    fun retry() {
        loadDetail()
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
        mutableUiState.update { it.copy(selectedEpisode = episode) }
    }

    fun watchOnTv() {
        val state = mutableUiState.value
        val episode = state.selectedEpisode
        viewModelScope.launch {
            mutableUiState.update { it.copy(tvErrorMessage = null, tvSuccessMessage = null) }

            val config = tvRepository.getConfig()
            if (config == null || !config.isPaired) {
                mutableUiState.update {
                    it.copy(tvErrorMessage = "Configura tu TV primero")
                }
                return@launch
            }

            val url = if (episode != null) {
                PlaybackUrlBuilder.buildTvUrl(tvShowId, episode.seasonNumber, episode.episodeNumber)
            } else {
                PlaybackUrlBuilder.buildTvUrl(tvShowId)
            }
            val result = tvRepository.openUrl(url)
            result.fold(
                onSuccess = {
                    mutableUiState.update {
                        it.copy(tvSuccessMessage = "Enviado a la TV")
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(tvErrorMessage = error.message ?: "No se pudo abrir en la TV")
                    }
                },
            )
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getTvShowDetail(tvShowId) }
                .onSuccess { detail ->
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
}
