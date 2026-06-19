package com.kastlg.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Inicio", Icons.Default.Home),
    Favorites("favorites", "Favoritos", Icons.Default.Favorite),
    History("history", "Historial", Icons.Default.History),
    Settings("settings", "Ajustes", Icons.Default.Settings),
    About("about", "Acerca de", Icons.Default.Info),
}

object DetailRoutes {
    const val BASE = "detail/{movieId}"
    fun create(movieId: Int) = "detail/$movieId"
}

object TvShowDetailRoutes {
    const val BASE = "tvshow/{tvShowId}"
    fun create(tvShowId: Int) = "tvshow/$tvShowId"
}

object TvSettingsRoutes {
    const val BASE = "tv-settings"
}
