package com.kastlg.app.presentation.theme

import androidx.compose.ui.graphics.Color

object KastLgColors {
    // Fondo principal (gradiente oscuro del logo)
    val Background = Color(0xFF0E0E12)
    val BackgroundRaised = Color(0xFF18181E)

    // Superficies (cuerpo del ghost: gris plata)
    val Surface = Color(0xFF1E1E24)
    val SurfaceElevated = Color(0xFF26262E)

    // Accent: color del ghost body — plata luminosa
    val Accent = Color(0xFFC8C8CC)
    val AccentMuted = Color(0xFF2A2A30)

    // Texto
    val TextPrimary = Color(0xFFF0F0F2)
    val TextSecondary = Color(0xFFA0A0A8)

    // Estados
    val Error = Color(0xFFFF6B6B)
    val Success = Color(0xFF6BCB77)

    // Legacy alias (mantiene compatibilidad con código existente)
    val ErrorContainer = Color(0xFF3A1A1A)
    val SuccessContainer = Color(0xFF1A2E1A)
}
