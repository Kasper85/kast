package com.kastlg.app.presentation.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kastlg.app.presentation.theme.KastLgColors

@Composable
fun AboutRoute(onBack: () -> Unit) {
    val context = LocalContext.current

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

        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = KastLgColors.Accent,
            modifier = Modifier.size(48.dp),
        )

        Text(
            text = "Acerca de Kast",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        InfoRow("Nombre", "Kast")
        InfoRow("Versi\u00f3n", "1.6")
        InfoRow("Desarrollado por", "Julio Rodr\u00edguez")
        InfoRow("Pel\u00edculas y series", "TMDB")
        InfoRow("Reproducci\u00f3n", "UnlimPlay")
        InfoRow("TV compatible", "LG webOS")
        InfoRow("Estado", "Beta / Proyecto personal")

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KastLgColors.Surface,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Kast utiliza TMDB para mostrar informaci\u00f3n de pel\u00edculas y series. La reproducci\u00f3n se realiza mediante UnlimPlay. Kast no aloja contenido multimedia ni distribuye archivos de v\u00eddeo.",
                style = MaterialTheme.typography.bodyMedium,
                color = KastLgColors.TextSecondary,
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/settings/api")))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KastLgColors.Accent,
                contentColor = KastLgColors.Background,
            ),
        ) {
            Text("Obtener API Key de TMDB")
        }

        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/")))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("TMDB")
        }

        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://unlimplay.com/")))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("UnlimPlay")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Reproducci\u00f3n proporcionada mediante UnlimPlay. Kast no controla la disponibilidad del contenido ni aloja archivos multimedia.",
            style = MaterialTheme.typography.bodySmall,
            color = KastLgColors.TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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
            fontWeight = FontWeight.Medium,
            color = KastLgColors.TextPrimary,
        )
    }
}
