package com.kastlg.app.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kastlg.app.di.AppContainer
import com.kastlg.app.domain.models.Genre
import com.kastlg.app.domain.models.Movie
import com.kastlg.app.domain.models.TvShow
import com.kastlg.app.presentation.components.CarouselSectionSkeleton
import com.kastlg.app.presentation.theme.KastLgColors
import java.util.Locale

@Composable
fun HomeRoute(
    onMovieClick: (Int) -> Unit = {},
    onTvShowClick: (Int) -> Unit = {},
    onConfigureToken: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            getTrendingMovies = AppContainer.getTrendingMovies,
            getNowPlayingMovies = AppContainer.getNowPlayingMovies,
            getTopRatedMovies = AppContainer.getTopRatedMovies,
            getPopularTvShows = AppContainer.getPopularTvShows,
            searchMovies = AppContainer.searchMovies,
            searchTvShows = AppContainer.searchTvShows,
            getMovieGenres = AppContainer.getMovieGenres,
            getTvGenres = AppContainer.getTvGenres,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onTabSelected = viewModel::onTabSelected,
        onMovieGenreSelected = viewModel::onMovieGenreSelected,
        onTvGenreSelected = viewModel::onTvGenreSelected,
        onRetry = viewModel::retry,
        onMovieClick = onMovieClick,
        onTvShowClick = onTvShowClick,
        onConfigureToken = onConfigureToken,
    )
}

private data class HomeContentState(
    val isSearch: Boolean,
    val isLoading: Boolean,
    val hasError: Boolean,
    val hasContent: Boolean,
)

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onQueryChange: (String) -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    onMovieGenreSelected: (Int?) -> Unit,
    onTvGenreSelected: (Int?) -> Unit,
    onRetry: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onConfigureToken: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(KastLgColors.Background, KastLgColors.BackgroundRaised),
                ),
            ),
    ) {
        // Search bar
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = onQueryChange,
        )

        if (uiState.searchQuery.isBlank()) {
            // Tab row
            TabRow(
                selectedTabIndex = HomeTab.entries.indexOf(uiState.selectedTab),
                containerColor = Color.Transparent,
                contentColor = KastLgColors.Accent,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[HomeTab.entries.indexOf(uiState.selectedTab)]),
                            color = KastLgColors.Accent,
                        )
                    }
                },
                divider = {},
            ) {
                HomeTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            // Genre chips
            if (uiState.selectedTab == HomeTab.Movies && uiState.movieGenres.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedMovieGenreId == null,
                            onClick = { onMovieGenreSelected(null) },
                            label = { Text("Todos") },
                        )
                    }
                    items(uiState.movieGenres, key = { it.id }) { genre ->
                        FilterChip(
                            selected = uiState.selectedMovieGenreId == genre.id,
                            onClick = { onMovieGenreSelected(genre.id) },
                            label = { Text(genre.name) },
                        )
                    }
                }
            }
            if (uiState.selectedTab == HomeTab.Series && uiState.tvGenres.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTvGenreId == null,
                            onClick = { onTvGenreSelected(null) },
                            label = { Text("Todos") },
                        )
                    }
                    items(uiState.tvGenres, key = { it.id }) { genre ->
                        FilterChip(
                            selected = uiState.selectedTvGenreId == genre.id,
                            onClick = { onTvGenreSelected(genre.id) },
                            label = { Text(genre.name) },
                        )
                    }
                }
            }
        }

        AnimatedContent(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            targetState = HomeContentState(
                isSearch = uiState.searchQuery.isNotBlank(),
                isLoading = uiState.isLoading && !uiState.hasContent,
                hasError = uiState.errorMessage != null,
                hasContent = uiState.hasContent,
            ),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "content",
        ) { state ->
            when {
                state.isSearch -> {
                    SearchResults(
                        uiState = uiState,
                        onMovieClick = onMovieClick,
                        onTvShowClick = onTvShowClick,
                        onConfigureToken = onConfigureToken,
                    )
                }
                state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                    ) {
                        CarouselSectionSkeleton("Tendencias")
                        CarouselSectionSkeleton("Estrenos")
                        CarouselSectionSkeleton("Mejor valoradas")
                    }
                }
                state.hasError -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = KastLgColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("Reintentar")
                        }
                        if ((uiState.errorMessage ?: "").contains("token", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onConfigureToken) {
                                Text("Configurar token TMDB")
                            }
                        }
                    }
                }
                state.hasContent -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        if (uiState.selectedTab == HomeTab.Movies) {
                            // Trending
                            if (uiState.trendingMovies.isNotEmpty()) {
                                item {
                                    CarouselSection(
                                        title = "Tendencias",
                                        items = uiState.trendingMovies,
                                        onItemClick = onMovieClick,
                                    )
                                }
                            }

                            // Now Playing
                            if (uiState.nowPlayingMovies.isNotEmpty()) {
                                item {
                                    CarouselSection(
                                        title = "Estrenos",
                                        items = uiState.nowPlayingMovies,
                                        onItemClick = onMovieClick,
                                    )
                                }
                            }

                            // Top Rated
                            if (uiState.topRatedMovies.isNotEmpty()) {
                                item {
                                    CarouselSection(
                                        title = "Mejor valoradas",
                                        items = uiState.topRatedMovies,
                                        onItemClick = onMovieClick,
                                    )
                                }
                            }
                        }

                        if (uiState.selectedTab == HomeTab.Series) {
                            // Popular TV Shows
                            if (uiState.popularTvShows.isNotEmpty()) {
                                item {
                                    TvCarouselSection(
                                        title = "Series populares",
                                        items = uiState.popularTvShows,
                                        onItemClick = onTvShowClick,
                                    )
                                }
                            }
                        }

                        // TMDB disclaimer
                        item {
                            Text(
                                text = "TMDB API · No avalado ni certificado por TMDB",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = KastLgColors.TextSecondary.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { contentDescription = "Campo de búsqueda de películas y series" },
        singleLine = true,
        placeholder = { Text("Buscar películas o series...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = KastLgColors.TextSecondary,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KastLgColors.Accent,
            unfocusedBorderColor = KastLgColors.TextSecondary.copy(alpha = 0.3f),
            focusedContainerColor = KastLgColors.Surface,
            unfocusedContainerColor = KastLgColors.Surface,
        ),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun CarouselSection(
    title: String,
    items: List<Movie>,
    onItemClick: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = Movie::id) { movie ->
                MovieCarouselCard(
                    movie = movie,
                    onClick = { onItemClick(movie.id) },
                )
            }
        }
    }
}

@Composable
private fun TvCarouselSection(
    title: String,
    items: List<TvShow>,
    onItemClick: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = TvShow::id) { tvShow ->
                TvShowCarouselCard(
                    tvShow = tvShow,
                    onClick = { onItemClick(tvShow.id) },
                )
            }
        }
    }
}

@Composable
private fun MovieCarouselCard(
    movie: Movie,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KastLgColors.Surface),
    ) {
        Column {
            if (movie.posterUrl != null) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = KastLgColors.TextSecondary,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(8.dp).height(72.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = movie.releaseDate.take(4).ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = KastLgColors.TextSecondary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = KastLgColors.Accent,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", movie.voteAverage),
                            style = MaterialTheme.typography.bodySmall,
                            color = KastLgColors.Accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvShowCarouselCard(
    tvShow: TvShow,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KastLgColors.Surface),
    ) {
        Column {
            if (tvShow.posterUrl != null) {
                AsyncImage(
                    model = tvShow.posterUrl,
                    contentDescription = tvShow.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = KastLgColors.TextSecondary,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(8.dp).height(72.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = tvShow.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = tvShow.releaseDate.take(4).ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = KastLgColors.TextSecondary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = KastLgColors.Accent,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", tvShow.voteAverage),
                            style = MaterialTheme.typography.bodySmall,
                            color = KastLgColors.Accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    uiState: HomeUiState,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onConfigureToken: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = "Cargando contenido" },
                color = MaterialTheme.colorScheme.primary,
            )
        }
        return
    }

    if (uiState.errorMessage != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(uiState.errorMessage, color = KastLgColors.TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.errorMessage.contains("token", ignoreCase = true)) {
                Button(onClick = onConfigureToken) { Text("Configurar token TMDB") }
            }
        }
        return
    }

    if (!uiState.hasSearchResults) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No se encontraron resultados",
                style = MaterialTheme.typography.bodyLarge,
                color = KastLgColors.TextSecondary,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "Películas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(uiState.searchResults, key = { "movie_${it.id}" }) { movie ->
                SearchMovieItem(movie = movie, onClick = { onMovieClick(movie.id) })
            }
        }

        if (uiState.searchTvResults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Series",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(uiState.searchTvResults, key = { "tv_${it.id}" }) { tvShow ->
                SearchTvShowItem(tvShow = tvShow, onClick = { onTvShowClick(tvShow.id) })
            }
        }
    }
}

@Composable
private fun SearchMovieItem(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KastLgColors.Surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (movie.posterUrl != null) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = KastLgColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Película · ${movie.releaseDate.take(4).ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KastLgColors.TextSecondary,
                )
            }
            Text(
                text = String.format(Locale.US, "%.1f", movie.voteAverage),
                style = MaterialTheme.typography.bodySmall,
                color = KastLgColors.Accent,
            )
        }
    }
}

@Composable
private fun SearchTvShowItem(tvShow: TvShow, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KastLgColors.Surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tvShow.posterUrl != null) {
                AsyncImage(
                    model = tvShow.posterUrl,
                    contentDescription = tvShow.title,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = KastLgColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tvShow.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Serie · ${tvShow.releaseDate.take(4).ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KastLgColors.TextSecondary,
                )
            }
            Text(
                text = String.format(Locale.US, "%.1f", tvShow.voteAverage),
                style = MaterialTheme.typography.bodySmall,
                color = KastLgColors.Accent,
            )
        }
    }
}
