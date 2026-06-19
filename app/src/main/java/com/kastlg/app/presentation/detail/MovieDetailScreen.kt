package com.kastlg.app.presentation.detail

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kastlg.app.domain.models.Genre
import com.kastlg.app.presentation.theme.KastLgColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailRoute(
    onBack: () -> Unit,
    onWatchOnTv: () -> Unit,
    viewModel: MovieDetailViewModel,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier.height(52.dp),
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

                uiState.errorMessage != null -> ErrorDetailState(
                    message = uiState.errorMessage.orEmpty(),
                    onRetry = viewModel::retry,
                    onBack = onBack,
                )

                else -> MovieDetailContent(
                    uiState = uiState,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onWatchOnTv = onWatchOnTv,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieDetailContent(
    uiState: MovieDetailUiState,
    onToggleFavorite: () -> Unit,
    onWatchOnTv: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    listOf(KastLgColors.Background, KastLgColors.BackgroundRaised),
                ),
            ),
    ) {
        // Poster — larger but not cropped
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            if (uiState.posterUrl != null) {
                AsyncImage(
                    model = uiState.posterUrl,
                    contentDescription = "Póster de ${uiState.title}",
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
                            contentDescription = "Póster no disponible",
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
            modifier = Modifier.padding(horizontal = 20.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Year + Rating row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Year
            Text(
                text = uiState.releaseYear,
                style = MaterialTheme.typography.bodyLarge,
                color = KastLgColors.TextSecondary,
            )

            // Rating
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
                    text = String.format(Locale.US, "%.1f", uiState.voteAverage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = KastLgColors.Accent,
                )
            }
        }

        // Genres
        if (uiState.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
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

        val persistenceError = uiState.persistenceErrorMessage
        if (persistenceError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = persistenceError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Synopsis
        if (uiState.overview.isNotBlank()) {
            Text(
                text = "Sinopsis",
                style = MaterialTheme.typography.titleMedium,
                color = KastLgColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.overview,
                style = MaterialTheme.typography.bodyLarge,
                color = KastLgColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TV message (shown before action buttons so it's visible without scrolling)
        val tvMessage = uiState.tvMessage
        if (tvMessage != null) {
            val isError = uiState.tvErrorMessage != null
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Favorite button
            OutlinedButton(
                onClick = onToggleFavorite,
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

            Button(
                onClick = onWatchOnTv,
                modifier = Modifier.weight(1f),
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
                Text("Ver en TV")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ErrorDetailState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No se pudo cargar la película",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = KastLgColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) {
            Text("Volver")
        }
    }
}
