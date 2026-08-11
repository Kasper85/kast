package com.kastlg.app.presentation.flixcorn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kastlg.app.data.remote.flixcorn.StreamingServer
import com.kastlg.app.presentation.theme.KastLgColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlixcornEpisodeRoute(
    slug: String,
    season: Int,
    episode: Int,
    onBack: () -> Unit,
    viewModel: FlixcornEpisodeViewModel = viewModel(
        factory = FlixcornEpisodeViewModelFactory(
            slug = slug,
            season = season,
            episode = episode,
        ),
    ),
) {
    val uiState by viewModel.uiState

    LaunchedEffect(slug, season, episode) {
        viewModel.loadServers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("S${season}E$episode - Servidores")
                },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // TV feedback banner
            val tvMessage = uiState.tvErrorMessage ?: uiState.tvSuccessMessage
            if (tvMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (uiState.tvErrorMessage != null) {
                        Color(0xFF3A1B1B)
                    } else {
                        Color(0xFF1B3A1B)
                    },
                ) {
                    Text(
                        text = tvMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = KastLgColors.Accent)
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
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
                        Button(onClick = { viewModel.loadServers() }) {
                            Text("Reintentar")
                        }
                    }
                }
                uiState.servers.isEmpty() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No se encontraron servidores",
                            style = MaterialTheme.typography.bodyLarge,
                            color = KastLgColors.TextSecondary,
                        )
                    }
                }
                else -> {
                    ServerList(
                        servers = uiState.servers,
                        selectedLanguage = uiState.selectedLanguage,
                        onLanguageSelected = { viewModel.selectLanguage(it) },
                        onServerClick = { viewModel.sendToTv(it) },
                        tvSending = uiState.tvSending,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServerList(
    servers: List<StreamingServer>,
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onServerClick: (StreamingServer) -> Unit,
    tvSending: Boolean,
    modifier: Modifier = Modifier,
) {
    val languages = remember(servers) {
        servers.map { it.language }.distinct().sorted()
    }

    val filteredServers = remember(servers, selectedLanguage) {
        if (selectedLanguage == null) servers
        else servers.filter { it.language == selectedLanguage }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Language filters
        if (languages.size > 1) {
            item {
                Text(
                    text = "Idioma",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = KastLgColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedLanguage == null,
                        onClick = { onLanguageSelected(null) },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KastLgColors.Accent.copy(alpha = 0.15f),
                            selectedLabelColor = KastLgColors.Accent,
                        ),
                    )
                    languages.forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { onLanguageSelected(lang) },
                            label = { Text(lang) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KastLgColors.Accent.copy(alpha = 0.15f),
                                selectedLabelColor = KastLgColors.Accent,
                            ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Server cards
        items(filteredServers, key = { "${it.serverName}_${it.quality}_${it.language}" }) { server ->
            ServerCard(
                server = server,
                enabled = !tvSending,
                onClick = { onServerClick(server) },
            )
        }
    }
}

@Composable
private fun ServerCard(server: StreamingServer, enabled: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) KastLgColors.Surface else KastLgColors.Surface.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KastLgColors.Accent.copy(alpha = 0.12f),
            ) {
                Icon(
                    Icons.Default.OndemandVideo,
                    contentDescription = null,
                    tint = KastLgColors.Accent,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.serverName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = KastLgColors.Accent.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = server.quality,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = KastLgColors.Accent,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = KastLgColors.TextSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = server.language,
                            style = MaterialTheme.typography.labelSmall,
                            color = KastLgColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
