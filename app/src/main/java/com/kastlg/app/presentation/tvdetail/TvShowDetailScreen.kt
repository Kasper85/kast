package com.kastlg.app.presentation.tvdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kastlg.app.domain.models.Episode
import com.kastlg.app.presentation.theme.KastLgColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TvShowDetailRoute(
    onBack: () -> Unit,
    onWatchOnTv: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeSelected: (Episode) -> Unit,
    viewModel: TvShowDetailViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = KastLgColors.TextPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.height(48.dp),
            )
        },
        containerColor = KastLgColors.Background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )

                uiState.errorMessage != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No se pudo cargar la serie",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = KastLgColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = viewModel::retry) { Text("Reintentar") }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedButton(onClick = onBack) { Text("Volver") }
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(
                            Brush.verticalGradient(
                                listOf(KastLgColors.Background, KastLgColors.BackgroundRaised),
                            ),
                        ),
                ) {
                    // Poster
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        if (uiState.posterUrl != null) {
                            AsyncImage(
                                model = uiState.posterUrl,
                                contentDescription = uiState.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        tint = KastLgColors.TextSecondary,
                                        modifier = Modifier.size(64.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Year + Rating + Seasons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.releaseYear,
                            style = MaterialTheme.typography.bodyLarge,
                            color = KastLgColors.TextSecondary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = KastLgColors.Accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = String.format("%.1f", uiState.voteAverage),
                                style = MaterialTheme.typography.bodyLarge,
                                color = KastLgColors.Accent,
                            )
                        }
                        if (uiState.numberOfSeasons > 0) {
                            Text(
                                text = "${uiState.numberOfSeasons} temp.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = KastLgColors.TextSecondary,
                            )
                        }
                    }

                    // Status
                    if (uiState.status.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KastLgColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }

                    // Genres
                    if (uiState.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.genres.forEach { genre ->
                                Surface(
                                    color = KastLgColors.AccentMuted,
                                    shape = AssistChipDefaults.shape,
                                ) {
                                    Text(
                                        text = genre.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = KastLgColors.Accent,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Synopsis
                    if (uiState.overview.isNotBlank()) {
                        Text(
                            text = "Sinopsis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KastLgColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = KastLgColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Seasons & Episodes
                    if (uiState.numberOfSeasons > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = KastLgColors.TextSecondary.copy(alpha = 0.2f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Temporadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KastLgColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Season chips
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (seasonNum in 1..uiState.numberOfSeasons) {
                                val isSelected = uiState.selectedSeason?.seasonNumber == seasonNum
                                AssistChip(
                                    onClick = { onSeasonSelected(seasonNum) },
                                    label = {
                                        Text(
                                            if (isSelected) "Temp. $seasonNum" else "$seasonNum",
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) KastLgColors.Accent else KastLgColors.AccentMuted,
                                        labelColor = if (isSelected) KastLgColors.Background else KastLgColors.Accent,
                                    ),
                                )
                            }
                        }

                        // Selected season episodes
                        if (uiState.isLoadingSeason) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                        }

                        val season = uiState.selectedSeason
                        if (season != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = season.name.ifBlank { "Temporada ${season.seasonNumber}" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = KastLgColors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            season.episodes.forEach { episode ->
                                EpisodeCard(
                                    episode = episode,
                                    isSelected = uiState.selectedEpisode?.id == episode.id,
                                    onClick = { onEpisodeSelected(episode) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    // TV Message
                    val tvMessage = uiState.tvMessage
                    if (tvMessage != null) {
                        val isError = uiState.tvErrorMessage != null
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B3A1B),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = tvMessage,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFE8F5E9),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Watch on TV button
                    Button(
                        onClick = onWatchOnTv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KastLgColors.Accent,
                            contentColor = KastLgColors.Background,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Ver en TV",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.selectedEpisode != null) "Ver episodio en TV" else "Ver en TV")
                    }

                    // Episode feedback
                    val selectedEpisode = uiState.selectedEpisode
                    if (selectedEpisode != null) {
                        Text(
                            text = "Temporada ${selectedEpisode.seasonNumber} · Episodio ${selectedEpisode.episodeNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KastLgColors.Accent,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    } else if (uiState.selectedSeason != null) {
                        Text(
                            text = "Selecciona un episodio para enviar a la TV",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KastLgColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }

                    // Favorites placeholder for series
                    Text(
                        text = "Próximamente: guardar series en favoritos",
                        style = MaterialTheme.typography.bodySmall,
                        color = KastLgColors.TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: Episode,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) KastLgColors.AccentMuted else KastLgColors.Surface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) KastLgColors.Accent else KastLgColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episode.airDate.isNotBlank()) {
                    Text(
                        text = episode.airDate.take(7),
                        style = MaterialTheme.typography.bodySmall,
                        color = KastLgColors.TextSecondary,
                    )
                }
            }
            if (episode.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = KastLgColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
