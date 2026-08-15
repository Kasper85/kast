package com.kastlg.app.presentation.flixcorn

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.di.AppContainer
import com.kastlg.app.domain.repositories.WatchedRepository
import com.kastlg.app.domain.usecases.GetFlixcornEpisodeServers
import com.kastlg.app.domain.usecases.GetFlixcornSeriesDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class FlixcornEpisodeUiState(
    val isLoading: Boolean = true,
    val servers: List<StreamingServer> = emptyList(),
    val selectedLanguage: String? = null,
    val error: String? = null,
    val tvSending: Boolean = false,
    val tvSuccessMessage: String? = null,
    val tvErrorMessage: String? = null,
    val isWatched: Boolean = false,
    val nextEpisode: Pair<Int, Int>? = null,
    val season: Int = 1,
    val episode: Int = 1,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FlixcornEpisodeViewModel(
    private val slug: String,
    private val season: Int,
    private val episode: Int,
    private val getEpisodeServers: GetFlixcornEpisodeServers,
    private val getSeriesDetail: GetFlixcornSeriesDetail,
    private val watchedRepository: WatchedRepository,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        FlixcornEpisodeUiState(season = season, episode = episode),
    )
    val uiState = _uiState

    private var currentSeason = season
    private var currentEpisode = episode
    private var seriesDetail: FlixcornSeriesDetail? = null

    private val episodeKey = MutableStateFlow(Triple(slug, season, episode))

    init {
        viewModelScope.launch {
            episodeKey
                .flatMapLatest { (s, seasonNumber, episodeNumber) ->
                    watchedRepository.observeIsEpisodeWatched(s, seasonNumber, episodeNumber)
                }
                .collect { isWatched ->
                    _uiState.value = _uiState.value.copy(isWatched = isWatched)
                }
        }
        refreshNextEpisode()
    }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getEpisodeServers(slug, currentSeason, currentEpisode)) {
                is FlixcornResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        servers = result.data,
                    )
                }
                is FlixcornResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No se pudieron cargar los servidores. Intenta de nuevo.",
                    )
                }
                is FlixcornResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun loadNextEpisode() {
        val next = _uiState.value.nextEpisode ?: return
        val nextSeason = next.first
        val nextEpisode = next.second
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getEpisodeServers(slug, nextSeason, nextEpisode)) {
                is FlixcornResult.Success -> {
                    updateCurrentEpisode(nextSeason, nextEpisode)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        servers = result.data,
                        season = nextSeason,
                        episode = nextEpisode,
                    )
                    recomputeNextEpisode()
                }
                is FlixcornResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No se pudieron cargar los servidores. Intenta de nuevo.",
                    )
                }
                is FlixcornResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun toggleWatched() {
        viewModelScope.launch {
            watchedRepository.toggleEpisode(slug, currentSeason, currentEpisode)
        }
    }

    fun selectLanguage(language: String?) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun sendToTv(server: StreamingServer) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                tvSending = true,
                tvErrorMessage = null,
                tvSuccessMessage = null,
            )

            // Check if TV is configured
            val config = AppContainer.tvRepository.getConfig()
            if (config == null || !config.isPaired) {
                _uiState.value = _uiState.value.copy(
                    tvSending = false,
                    tvErrorMessage = "Configura tu TV primero desde Ajustes.",
                )
                return@launch
            }

            // Resolve URL and send to TV
            when (val result = AppContainer.sendFlixcornToTv(server)) {
                is FlixcornResult.Success -> {
                    val url = result.data
                    val sendResult = AppContainer.tvRepository.openUrl(url)
                    if (sendResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            tvSending = false,
                            tvSuccessMessage = "Enviado a la TV",
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            tvSending = false,
                            tvErrorMessage = sendResult.exceptionOrNull()?.message
                                ?: "Error al enviar a la TV",
                        )
                    }
                }
                is FlixcornResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        tvSending = false,
                        tvErrorMessage = "No se pudo resolver la URL del video.",
                    )
                }
                is FlixcornResult.Loading -> {}
            }
        }
    }

    private fun refreshNextEpisode() {
        viewModelScope.launch {
            when (val result = getSeriesDetail(slug)) {
                is FlixcornResult.Success -> {
                    seriesDetail = result.data
                    recomputeNextEpisode()
                }
                is FlixcornResult.Error -> Unit
                is FlixcornResult.Loading -> Unit
            }
        }
    }

    private fun recomputeNextEpisode() {
        val detail = seriesDetail ?: return
        _uiState.value = _uiState.value.copy(
            nextEpisode = computeNextEpisode(detail, currentSeason, currentEpisode),
        )
    }

    private fun computeNextEpisode(
        detail: FlixcornSeriesDetail,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Pair<Int, Int>? {
        val currentSeasonDetail = detail.seasons.firstOrNull { it.seasonNumber == seasonNumber }
        val maxEpisode = currentSeasonDetail?.episodes?.maxOfOrNull { it.episodeNumber }
        if (maxEpisode != null && episodeNumber < maxEpisode) {
            return seasonNumber to (episodeNumber + 1)
        }
        return if (seasonNumber < detail.numberOfSeasons) (seasonNumber + 1) to 1 else null
    }

    private fun updateCurrentEpisode(seasonNumber: Int, episodeNumber: Int) {
        currentSeason = seasonNumber
        currentEpisode = episodeNumber
        episodeKey.value = Triple(slug, seasonNumber, episodeNumber)
    }
}

class FlixcornEpisodeViewModelFactory(
    private val slug: String,
    private val season: Int,
    private val episode: Int,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FlixcornEpisodeViewModel::class.java))
        return FlixcornEpisodeViewModel(
            slug = slug,
            season = season,
            episode = episode,
            getEpisodeServers = AppContainer.getFlixcornEpisodeServers,
            getSeriesDetail = AppContainer.getFlixcornSeriesDetail,
            watchedRepository = AppContainer.watchedRepository,
        ) as T
    }
}
