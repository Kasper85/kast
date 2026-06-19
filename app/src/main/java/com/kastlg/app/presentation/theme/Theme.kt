package com.kastlg.app.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = KastLgColors.Accent,
    onPrimary = KastLgColors.Background,
    primaryContainer = KastLgColors.AccentMuted,
    onPrimaryContainer = KastLgColors.TextPrimary,
    secondary = KastLgColors.Accent,
    onSecondary = KastLgColors.Background,
    secondaryContainer = KastLgColors.AccentMuted,
    onSecondaryContainer = KastLgColors.TextPrimary,
    tertiary = KastLgColors.Accent,
    onTertiary = KastLgColors.Background,
    background = KastLgColors.Background,
    onBackground = KastLgColors.TextPrimary,
    surface = KastLgColors.Surface,
    onSurface = KastLgColors.TextPrimary,
    surfaceVariant = KastLgColors.BackgroundRaised,
    onSurfaceVariant = KastLgColors.TextSecondary,
    surfaceTint = Color.Transparent,
    error = KastLgColors.Error,
    onError = Color.White,
    errorContainer = KastLgColors.ErrorContainer,
    onErrorContainer = KastLgColors.Error,
    outline = Color(0xFF444450),
    outlineVariant = Color(0xFF333340),
    inverseSurface = KastLgColors.TextPrimary,
    inverseOnSurface = KastLgColors.Background,
)

@Composable
fun KastLgTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KastLgColors.Background.toArgb()
            window.navigationBarColor = KastLgColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = KastLgTypography,
        content = content,
    )
}
