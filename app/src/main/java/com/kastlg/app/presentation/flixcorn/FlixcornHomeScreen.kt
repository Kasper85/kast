package com.kastlg.app.presentation.flixcorn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.presentation.theme.KastLgColors

@Composable
fun FlixcornHomeRoute(
    onSeriesClick: (String) -> Unit,
    viewModel: FlixcornHomeViewModel = viewModel(factory = FlixcornHomeViewModelFactory()),
) {
    val uiState by viewModel.uiState

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
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            singleLine = true,
            placeholder = { Text("Buscar series en Flixcorn...") },
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

        AnimatedVisibility(visible = uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = KastLgColors.Accent)
            }
        }

        if (uiState.results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.results, key = { it.slug }) { result ->
                    FlixcornSearchItem(result = result, onClick = { onSeriesClick(result.slug) })
                }
            }
        } else if (!uiState.isLoading && uiState.query.isNotBlank() && uiState.errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No se encontraron resultados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KastLgColors.TextSecondary,
                )
            }
        }

        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KastLgColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (!uiState.isLoading && uiState.query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Busca series de Flixcorn\ny reproducilas en tu TV",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KastLgColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FlixcornSearchItem(result: FlixcornSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (result.posterUrl != null) {
            AsyncImage(
                model = result.posterUrl,
                contentDescription = result.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KastLgColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = KastLgColors.TextSecondary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    result.year?.let { append(it.toString()) }
                    if (result.genres.isNotEmpty()) {
                        if (isNotEmpty()) append(" \u00b7 ")
                        append(result.genres.take(2).joinToString(", "))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = KastLgColors.TextSecondary,
            )
        }
    }
}
