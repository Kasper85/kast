package com.kastlg.app.presentation.tvsettings

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kastlg.app.presentation.theme.KastLgColors

@Composable
fun TvSettingsRoute(
    viewModel: TvSettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

        // Header
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = KastLgColors.Accent,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Configuración de TV",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Conecta tu TV LG webOS a la misma red WiFi.",
            style = MaterialTheme.typography.bodyLarge,
            color = KastLgColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Always show status indicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (uiState.isConnected) Color(0xFF1B3A1B) else KastLgColors.Surface,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (uiState.isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (uiState.isConnected) "TV conectada" else "TV desconectada",
                    tint = if (uiState.isConnected) Color(0xFF4CAF50) else KastLgColors.TextSecondary,
                )
                Column {
                    Text(
                        text = if (uiState.isConnected) "Conectada" else "Desconectada",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (uiState.isConnected) Color(0xFFE8F5E9) else KastLgColors.TextSecondary,
                    )
                    if (uiState.isConnected) {
                        Text(
                            text = uiState.config?.tvName ?: uiState.config?.tvIp ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA5D6A7),
                        )
                    }
                }
            }
        }

        // Discovery button
        if (!uiState.isConnected) {
            OutlinedButton(
                onClick = viewModel::discoverTvs,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning && !uiState.isConnecting,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isScanning) {
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
            uiState.discoveredTvs.forEach { tv ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectDiscoveredTv(tv) },
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
            value = uiState.tvIp,
            onValueChange = viewModel::onIpChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("IP de la TV") },
            placeholder = { Text("192.168.1.100") },
            singleLine = true,
            enabled = !uiState.isConnecting,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KastLgColors.Accent,
                unfocusedBorderColor = KastLgColors.TextSecondary,
            ),
        )

        // Name input
        OutlinedTextField(
            value = uiState.tvName,
            onValueChange = viewModel::onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de la TV") },
            placeholder = { Text("Sala de estar") },
            singleLine = true,
            enabled = !uiState.isConnecting,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KastLgColors.Accent,
                unfocusedBorderColor = KastLgColors.TextSecondary,
            ),
        )

        // Connect button
        Button(
            onClick = viewModel::connect,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isConnecting && uiState.tvIp.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KastLgColors.Accent,
                contentColor = KastLgColors.Background,
            ),
        ) {
            if (uiState.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = KastLgColors.Background,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(if (uiState.isPairing) "Esperando la TV..." else "Conectando...")
            } else {
                Text(if (uiState.isConnected) "Reconectar" else "Conectar a la TV")
            }
        }

        // Delete config button
        if (uiState.isConnected) {
            OutlinedButton(
                onClick = { viewModel.deleteConfig() },
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

        // Messages
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
private fun Row(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
    ) {
        content()
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
