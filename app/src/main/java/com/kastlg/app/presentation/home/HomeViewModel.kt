package com.kastlg.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.domain.repositories.MissingTmdbTokenException
import com.kastlg.app.domain.usecases.GetMovieGenresUseCase
import com.kastlg.app.domain.usecases.GetNowPlayingMoviesUseCase
import com.kastlg.app.domain.usecases.GetPopularTvShowsUseCase
import com.kastlg.app.domain.usecases.GetTopRatedMoviesUseCase
import com.kastlg.app.domain.usecases.GetTrendingMoviesUseCase
import com.kastlg.app.domain.usecases.GetTvGenresUseCase
import com.kastlg.app.domain.usecases.SearchMoviesUseCase
import com.kastlg.app.domain.usecases.SearchTvShowsUseCase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HomeViewModel(
    private val getTrendingMovies: GetTrendingMoviesUseCase,
    private val getNowPlayingMovies: GetNowPlayingMoviesUseCase,
    private val getTopRatedMovies: GetTopRatedMoviesUseCase,
    private val getPopularTvShows: GetPopularTvShowsUseCase,
    private val searchMovies: SearchMoviesUseCase,
    private val searchTvShows: SearchTvShowsUseCase,
    private val getMovieGenres: GetMovieGenresUseCase,
    private val getTvGenres: GetTvGenresUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState
    private var searchJob: Job? = null
    private var categoryJob: Job? = null

    // Unfiltered data for genre filtering
    private var allTrendingMovies: List<Movie> = emptyList()
    private var allNowPlayingMovies: List<Movie> = emptyList()
    private var allTopRatedMovies: List<Movie> = emptyList()
    private var allPopularTvShows: List<TvShow> = emptyList()

    init {
        loadAllCategories()
        loadGenres()
    }

    fun onQueryChange(query: String) {
        mutableUiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            searchJob?.cancel()
            categoryJob?.cancel()
            loadAllCategories()
        } else {
            categoryJob?.cancel()
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                // Wait for any running category load to finish before starting search
                categoryJob?.join()
                delay(SEARCH_DEBOUNCE_MILLIS)
                search(query)
            }
        }
    }

    fun onTabSelected(tab: HomeTab) {
        mutableUiState.update { it.copy(selectedTab = tab) }
    }

    fun onMovieGenreSelected(genreId: Int?) {
        mutableUiState.update { it.copy(selectedMovieGenreId = genreId) }
        applyMovieGenreFilter(genreId)
    }

    fun onTvGenreSelected(genreId: Int?) {
        mutableUiState.update { it.copy(selectedTvGenreId = genreId) }
        applyTvGenreFilter(genreId)
    }

    fun retry() {
        allTrendingMovies = emptyList()
        allNowPlayingMovies = emptyList()
        allTopRatedMovies = emptyList()
        allPopularTvShows = emptyList()
        loadAllCategories()
        loadGenres()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            val movies = runCatching { getMovieGenres() }.getOrDefault(emptyList())
            val tv = runCatching { getTvGenres() }.getOrDefault(emptyList())
            mutableUiState.update {
                it.copy(movieGenres = movies, tvGenres = tv)
            }
        }
    }

    private fun applyMovieGenreFilter(genreId: Int?) {
        val filtered = if (genreId == null) {
            Triple(allTrendingMovies, allNowPlayingMovies, allTopRatedMovies)
        } else {
            Triple(
                allTrendingMovies.filter { it.genreIds.contains(genreId) },
                allNowPlayingMovies.filter { it.genreIds.contains(genreId) },
                allTopRatedMovies.filter { it.genreIds.contains(genreId) },
            )
        }
        mutableUiState.update {
            it.copy(
                trendingMovies = filtered.first,
                nowPlayingMovies = filtered.second,
                topRatedMovies = filtered.third,
            )
        }
    }

    private fun applyTvGenreFilter(genreId: Int?) {
        val filtered = if (genreId == null) {
            allPopularTvShows
        } else {
            allPopularTvShows.filter { it.genreIds.contains(genreId) }
        }
        mutableUiState.update {
            it.copy(popularTvShows = filtered)
        }
    }

    private fun loadAllCategories() {
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            val queryBeforeLoad = mutableUiState.value.searchQuery
            mutableUiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            try {
                val trending = async { runCatching { getTrendingMovies() }.getOrDefault(emptyList()) }
                val nowPlaying = async { runCatching { getNowPlayingMovies() }.getOrDefault(emptyList()) }
                val topRated = async { runCatching { getTopRatedMovies() }.getOrDefault(emptyList()) }
                val popularTv = async { runCatching { getPopularTvShows() }.getOrDefault(emptyList()) }

                // Only update categories if the user hasn't started typing
                if (mutableUiState.value.searchQuery == queryBeforeLoad) {
                    allTrendingMovies = trending.await()
                    allNowPlayingMovies = nowPlaying.await()
                    allTopRatedMovies = topRated.await()
                    allPopularTvShows = popularTv.await()

                    // Re-apply current genre filter
                    val currentMovieGenre = mutableUiState.value.selectedMovieGenreId
                    val currentTvGenre = mutableUiState.value.selectedTvGenreId
                    applyMovieGenreFilter(currentMovieGenre)
                    applyTvGenreFilter(currentTvGenre)

                    mutableUiState.update {
                        it.copy(isLoading = false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (mutableUiState.value.searchQuery == queryBeforeLoad) {
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.toActionableMessage(),
                        )
                    }
                }
            }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isLoading = true,
                    trendingMovies = emptyList(),
                    nowPlayingMovies = emptyList(),
                    topRatedMovies = emptyList(),
                    popularTvShows = emptyList(),
                    errorMessage = null,
                )
            }

            try {
                val movies = async { searchMovies(query) }
                val tvShows = async { searchTvShows(query) }

                mutableUiState.update {
                    it.copy(
                        searchResults = movies.await(),
                        searchTvResults = tvShows.await(),
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toActionableMessage(),
                    )
                }
            }
        }
    }

    private fun Throwable.toActionableMessage(): String = when (this) {
        is MissingTmdbTokenException -> message.orEmpty()
        is IOException -> "No se pudo conectar con TMDB. Verifica tu conexi\u00f3n a internet."
        is HttpException -> when (code()) {
            401 -> "TMDB rechaz\u00f3 el token. Configura el token TMDB desde Ajustes."
            429 -> "TMDB recibi\u00f3 demasiadas solicitudes. Espera un momento e intenta de nuevo."
            else -> "TMDB devolvi\u00f3 un error (${code()}). Intenta de nuevo en breve."
        }
        else -> "No se pudieron cargar los datos. Intenta de nuevo."
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
    }
}
