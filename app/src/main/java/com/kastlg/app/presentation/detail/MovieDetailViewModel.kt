package com.kastlg.app.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.tv.PlaybackUrlBuilder
import com.kastlg.app.domain.models.MovieDetail
import com.kastlg.app.domain.repositories.FavoriteRepository
import com.kastlg.app.domain.repositories.HistoryRepository
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import com.kastlg.app.domain.repositories.TvRepository
import com.kastlg.app.domain.usecases.GetMovieDetailUseCase
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

class MovieDetailViewModel(
    private val movieId: Int,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val tvRepository: TvRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MovieDetailUiState())
    private var loadedDetail: MovieDetail? = null

    val uiState: StateFlow<MovieDetailUiState> = mutableUiState

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent

    sealed class NavigationEvent {
        data object NavigateToTvSettings : NavigationEvent()
    }

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
                favoriteRepository.toggle(detail)
                mutableUiState.update { it.copy(favoriteErrorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                mutableUiState.update {
                    it.copy(favoriteErrorMessage = FAVORITE_PERSISTENCE_ERROR)
                }
            }
        }
    }

    fun watchOnTv() {
        val detail = loadedDetail ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(tvErrorMessage = null, tvSuccessMessage = null) }

            val config = tvRepository.getConfig()
            if (config == null || !config.isPaired) {
                _navigationEvent.emit(NavigationEvent.NavigateToTvSettings)
                return@launch
            }

            val url = PlaybackUrlBuilder.buildMovieUrl(detail.id)
            val result = tvRepository.openUrl(url)
            result.fold(
                onSuccess = {
                    mutableUiState.update {
                        it.copy(tvSuccessMessage = "Enviado a la TV")
                    }
                    try {
                        historyRepository.recordSentToTv(detail)
                    } catch (_: Throwable) {
                        // Non-critical: TV playback succeeded even if history update failed
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

    private fun observeFavorite() {
        viewModelScope.launch {
            favoriteRepository.observeIsFavorite(movieId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableUiState.update {
                        it.copy(favoriteErrorMessage = FAVORITE_PERSISTENCE_ERROR)
                    }
                }
                .collect { isFavorite ->
                    mutableUiState.update {
                        it.copy(
                            isFavorite = isFavorite,
                            favoriteErrorMessage = null,
                        )
                    }
                }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getMovieDetail(movieId) }
                .onSuccess { detail ->
                    loadedDetail = detail
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            title = detail.title,
                            posterUrl = detail.posterUrl,
                            overview = detail.overview,
                            releaseYear = detail.releaseDate.take(4).ifBlank { "—" },
                            voteAverage = detail.voteAverage,
                            genres = detail.genres,
                            errorMessage = null,
                        )
                    }
                    try {
                        historyRepository.recordViewed(detail)
                        mutableUiState.update { it.copy(historyErrorMessage = null) }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        mutableUiState.update {
                            it.copy(historyErrorMessage = HISTORY_PERSISTENCE_ERROR)
                        }
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
            404 -> "Película no encontrada en TMDB."
            429 -> "TMDB recibió demasiadas solicitudes. Espera un momento e intenta de nuevo."
            else -> "TMDB devolvió un error (${code()}). Intenta de nuevo en breve."
        }
        else -> "No se pudieron cargar los detalles. Intenta de nuevo."
    }

    private companion object {
        const val FAVORITE_PERSISTENCE_ERROR =
            "No se pudo guardar el favorito. Intenta de nuevo."
        const val HISTORY_PERSISTENCE_ERROR =
            "No se pudo registrar en el historial."
    }
}
