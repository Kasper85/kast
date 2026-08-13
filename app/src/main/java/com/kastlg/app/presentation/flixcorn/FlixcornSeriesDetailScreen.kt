package com.kastlg.app.presentation.flixcorn

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.data.remote.flixcorn.FlixcornSeason
import com.kastlg.app.presentation.theme.KastLgColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlixcornSeriesDetailRoute(
    slug: String,
    onBack: () -> Unit,
    onEpisodeClick: (Int, Int) -> Unit,
    viewModel: FlixcornSeriesDetailViewModel = viewModel(
        factory = FlixcornSeriesDetailViewModelFactory(slug = slug),
    ),
) {
    val uiState by viewModel.uiState

    LaunchedEffect(slug) {
        viewModel.loadSeries()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.series?.title ?: "Flixcorn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = KastLgColors.Background,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = KastLgColors.Accent)
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KastLgColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadSeries() }) {
                        Text("Reintentar")
                    }
                }
            }
            uiState.series != null -> {
                SeriesDetailContent(
                    series = uiState.series!!,
                    onEpisodeClick = onEpisodeClick,
                    modifier = Modifier.padding(padding),
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun SeriesDetailContent(
    series: FlixcornSeriesDetail,
    onEpisodeClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlixcornSeriesDetailViewModel = remember { FlixcornSeriesDetailViewModel(slug = series.slug) },
) {
    val uiState = viewModel.uiState

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Backdrop + info header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                if (series.backdropUrl != null) {
                    AsyncImage(
                        model = series.backdropUrl,
                        contentDescription = series.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else if (series.posterUrl != null) {
                    AsyncImage(
                        model = series.posterUrl,
                        contentDescription = series.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    KastLgColors.Background,
                                ),
                                startY = 150f,
                            ),
                        ),
                )
            }
        }

        // Series info
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    series.year?.let {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = KastLgColors.TextSecondary,
                        )
                    }
                    Text(
                        text = series.status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KastLgColors.TextSecondary,
                    )
                    Text(
                        text = "${series.numberOfSeasons} temporada${if (series.numberOfSeasons != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KastLgColors.TextSecondary,
                    )
                }
                if (series.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        series.genres.take(4).forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = KastLgColors.Accent.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = genre,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KastLgColors.Accent,
                                )
                            }
                        }
                    }
                }
                if (series.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = series.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KastLgColors.TextSecondary,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Favorite button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel.toggleFavorite,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (uiState.isFavorite) KastLgColors.Error else KastLgColors.TextPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (uiState.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isFavorite) "En favoritos" else "Favorito")
                }
            }
        }

        // Seasons and episodes
        items(series.seasons, key = { it.seasonNumber }) { season ->
            SeasonSection(season = season, onEpisodeClick = onEpisodeClick)
        }
    }
}

@Composable
private fun SeasonSection(
    season: FlixcornSeason,
    onEpisodeClick: (Int, Int) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = "Temporada ${season.seasonNumber}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        season.episodes.forEach { episode ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { onEpisodeClick(season.seasonNumber, episode.episodeNumber) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = KastLgColors.Surface),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KastLgColors.Accent.copy(alpha = 0.12f),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = KastLgColors.Accent,
                            modifier = Modifier.padding(8.dp).size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = episode.title.ifBlank { "Episodio ${episode.episodeNumber}" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (episode.synopsis.isNotBlank()) {
                            Text(
                                text = episode.synopsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = KastLgColors.TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
