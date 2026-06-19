package com.kastlg.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kastlg.app.presentation.theme.KastLgColors
import com.kastlg.app.presentation.tvsettings.TvSettingsViewModel

@Composable
fun SettingsRoute(
    onNavigateToHome: () -> Unit,
    viewModel: SettingsViewModel,
    tvSettingsViewModel: TvSettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tvUiState by tvSettingsViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(KastLgColors.Background, KastLgColors.BackgroundRaised),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ====================================================================
        // SECTION 1: TMDB
        // ====================================================================
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            tint = KastLgColors.Accent,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Configura el token de TMDB para buscar películas.",
            style = MaterialTheme.typography.bodyLarge,
            color = KastLgColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Current token status
        if (uiState.hasToken) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1B3A1B),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Token configurado",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE8F5E9),
                        )
                        Text(
                            text = uiState.maskedToken,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA5D6A7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "No hay token configurado. La app no puede buscar películas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Token input
        OutlinedTextField(
            value = uiState.inputToken,
            onValueChange = viewModel::onInputChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Token TMDB") },
            placeholder = { Text("eyJhbGciOi...") },
            singleLine = true,
            visualTransformation = if (uiState.isTokenVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = viewModel::toggleTokenVisibility) {
                    Icon(
                        imageVector = if (uiState.isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (uiState.isTokenVisible) "Ocultar" else "Mostrar",
                        tint = KastLgColors.TextSecondary,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KastLgColors.Accent,
                unfocusedBorderColor = KastLgColors.TextSecondary,
            ),
        )

        // Help text
        Text(
            text = "Necesitas un API Read Access Token de TMDB. Normalmente empieza con eyJ...",
            style = MaterialTheme.typography.bodyMedium,
            color = KastLgColors.TextSecondary,
        )

        // TMDB API Key button
        val context = androidx.compose.ui.platform.LocalContext.current
        OutlinedButton(
            onClick = {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.themoviedb.org/settings/api")))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Obtener API Key de TMDB")
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = viewModel::saveToken,
                modifier = Modifier.weight(1f),
                enabled = uiState.inputToken.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KastLgColors.Accent,
                    contentColor = KastLgColors.Background,
                ),
            ) {
                Text("Guardar token")
            }

            if (uiState.hasToken) {
                OutlinedButton(
                    onClick = viewModel::clearToken,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = KastLgColors.Error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Borrar token")
                }
            }
        }

        // Go to Home button (when token is configured)
        if (uiState.hasToken) {
            OutlinedButton(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Ir a buscar películas")
            }
        }

        // ====================================================================
        // DIVIDER
        // ====================================================================
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = KastLgColors.TextSecondary.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        // ====================================================================
        // SECTION 2: TV
        // ====================================================================
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = KastLgColors.Accent,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "TV",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Conecta tu TV LG webOS a la misma red WiFi.",
            style = MaterialTheme.typography.bodyLarge,
            color = KastLgColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status indicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (tvUiState.isConnected) Color(0xFF1B3A1B) else KastLgColors.Surface,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (tvUiState.isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (tvUiState.isConnected) "TV conectada" else "TV desconectada",
                    tint = if (tvUiState.isConnected) Color(0xFF4CAF50) else KastLgColors.TextSecondary,
                )
                Column {
                    Text(
                        text = if (tvUiState.isConnected) "Conectada" else "Desconectada",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (tvUiState.isConnected) Color(0xFFE8F5E9) else KastLgColors.TextSecondary,
                    )
                    if (tvUiState.isConnected) {
                        Text(
                            text = tvUiState.config?.tvName ?: tvUiState.config?.tvIp ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA5D6A7),
                        )
                    }
                }
            }
        }

        // Discovery button (only when disconnected)
        if (!tvUiState.isConnected) {
            OutlinedButton(
                onClick = tvSettingsViewModel::discoverTvs,
                modifier = Modifier.fillMaxWidth(),
                enabled = !tvUiState.isScanning && !tvUiState.isConnecting,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (tvUiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = KastLgColors.Accent,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscando TVs...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar TVs en la red")
                }
            }

            // Discovered TVs list
            tvUiState.discoveredTvs.forEach { tv ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tvSettingsViewModel.selectDiscoveredTv(tv) },
                    color = KastLgColors.Surface,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = KastLgColors.Accent,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tv.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = KastLgColors.TextPrimary,
                            )
                            Text(
                                text = tv.ip,
                                style = MaterialTheme.typography.bodyMedium,
                                color = KastLgColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        // IP input
        OutlinedTextField(
            value = tvUiState.tvIp,
            onValueChange = tvSettingsViewModel::onIpChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("IP de la TV") },
            placeholder = { Text("192.168.1.100") },
            singleLine = true,
            enabled = !tvUiState.isConnecting,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KastLgColors.Accent,
                unfocusedBorderColor = KastLgColors.TextSecondary,
            ),
        )

        // Name input
        OutlinedTextField(
            value = tvUiState.tvName,
            onValueChange = tvSettingsViewModel::onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de la TV") },
            placeholder = { Text("Sala de estar") },
            singleLine = true,
            enabled = !tvUiState.isConnecting,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KastLgColors.Accent,
                unfocusedBorderColor = KastLgColors.TextSecondary,
            ),
        )

        // Connect button
        Button(
            onClick = tvSettingsViewModel::connect,
            modifier = Modifier.fillMaxWidth(),
            enabled = !tvUiState.isConnecting && tvUiState.tvIp.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KastLgColors.Accent,
                contentColor = KastLgColors.Background,
            ),
        ) {
            if (tvUiState.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = KastLgColors.Background,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(if (tvUiState.isPairing) "Esperando la TV..." else "Conectando...")
            } else {
                Text(if (tvUiState.isConnected) "Reconectar" else "Conectar a la TV")
            }
        }

        // Delete config button
        if (tvUiState.isConnected) {
            OutlinedButton(
                onClick = { tvSettingsViewModel.deleteConfig() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KastLgColors.Error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("Borrar configuración")
            }
        }

        // Help section
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KastLgColors.Surface,
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "¿Cómo funciona?",
                    style = MaterialTheme.typography.titleMedium,
                    color = KastLgColors.TextPrimary,
                )
                HelpItem("1. La TV y el celular deben estar en la misma red WiFi.")
                HelpItem("2. Usa \"Buscar TVs\" o ingresa la IP manualmente.")
                HelpItem("3. Presiona Conectar y acepta el permiso en la TV.")
                HelpItem("4. Envía películas desde la app a tu TV.")
            }
        }

        // TV messages
        tvUiState.errorMessage?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        tvUiState.successMessage?.let { success ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1B3A1B),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                    )
                    Text(
                        text = success,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE8F5E9),
                    )
                }
            }
        }

        // ====================================================================
        // TMDB messages (from SettingsViewModel)
        // ====================================================================
        uiState.errorMessage?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        uiState.successMessage?.let { success ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1B3A1B),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                    )
                    Text(
                        text = success,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE8F5E9),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = KastLgColors.TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = KastLgColors.TextPrimary,
        )
    }
}

@Composable
private fun HelpItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = KastLgColors.TextSecondary,
    )
}
