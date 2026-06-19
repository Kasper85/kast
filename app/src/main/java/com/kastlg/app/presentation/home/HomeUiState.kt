package com.kastlg.app.presentation.home

import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.TvShow

enum class HomeTab(val label: String) {
    Movies("Películas"),
    Series("Series"),
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTab: HomeTab = HomeTab.Movies,
    val trendingMovies: List<Movie> = emptyList(),
    val nowPlayingMovies: List<Movie> = emptyList(),
    val topRatedMovies: List<Movie> = emptyList(),
    val popularTvShows: List<TvShow> = emptyList(),
    val searchResults: List<Movie> = emptyList(),
    val searchTvResults: List<TvShow> = emptyList(),
    val selectedMovieGenreId: Int? = null,
    val selectedTvGenreId: Int? = null,
    val movieGenres: List<Genre> = emptyList(),
    val tvGenres: List<Genre> = emptyList(),
    val errorMessage: String? = null,
) {
    val isSearchMode: Boolean get() = searchQuery.isNotBlank()
    val hasContent: Boolean get() = trendingMovies.isNotEmpty() || nowPlayingMovies.isNotEmpty() ||
        topRatedMovies.isNotEmpty() || popularTvShows.isNotEmpty()
    val hasSearchResults: Boolean get() = searchResults.isNotEmpty() || searchTvResults.isNotEmpty()
}
