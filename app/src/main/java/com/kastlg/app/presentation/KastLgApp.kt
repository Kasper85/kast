package com.kastlg.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kastlg.app.di.AppContainer
import com.kastlg.app.presentation.about.AboutRoute
import com.kastlg.app.presentation.detail.MovieDetailRoute
import com.kastlg.app.presentation.detail.MovieDetailViewModelFactory
import com.kastlg.app.presentation.favorites.FavoritesRoute
import com.kastlg.app.presentation.history.HistoryRoute
import com.kastlg.app.presentation.home.HomeRoute
import com.kastlg.app.presentation.navigation.AppDestination
import com.kastlg.app.presentation.navigation.DetailRoutes
import com.kastlg.app.presentation.navigation.TvSettingsRoutes
import com.kastlg.app.presentation.navigation.TvShowDetailRoutes
import com.kastlg.app.presentation.settings.SettingsRoute
import com.kastlg.app.presentation.settings.SettingsViewModel
import com.kastlg.app.presentation.settings.SettingsViewModelFactory
import com.kastlg.app.presentation.theme.KastLgColors
import com.kastlg.app.presentation.tvdetail.TvShowDetailRoute
import com.kastlg.app.presentation.tvdetail.TvShowDetailViewModelFactory
import com.kastlg.app.presentation.tvsettings.TvSettingsRoute
import com.kastlg.app.presentation.tvsettings.TvSettingsViewModel
import com.kastlg.app.presentation.tvsettings.TvSettingsViewModelFactory

@Composable
fun KastLgApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    val isTabDestination = currentDestination?.route in AppDestination.entries.map { it.route }
    val isDetailScreen = currentDestination?.route?.startsWith("detail/") == true ||
        currentDestination?.route?.startsWith("tvshow/") == true
    val isAboutScreen = currentDestination?.route == AppDestination.About.route
    val showBottomBar = isTabDestination || isDetailScreen || isAboutScreen

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(AppDestination.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = KastLgColors.AccentMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Home.route) {
                HomeRoute(
                    onMovieClick = { movieId ->
                        navController.navigate(DetailRoutes.create(movieId))
                    },
                    onTvShowClick = { tvShowId ->
                        navController.navigate(TvShowDetailRoutes.create(tvShowId))
                    },
                    onConfigureToken = {
                        navController.navigate(AppDestination.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.Favorites.route) {
                FavoritesRoute(
                    onMovieClick = { movieId ->
                        navController.navigate(DetailRoutes.create(movieId))
                    },
                    onNavigateToHome = {
                        navController.navigate(AppDestination.Home.route) {
                            popUpTo(AppDestination.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.History.route) {
                HistoryRoute(
                    onMovieClick = { movieId ->
                        navController.navigate(DetailRoutes.create(movieId))
                    },
                    onNavigateToHome = {
                        navController.navigate(AppDestination.Home.route) {
                            popUpTo(AppDestination.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.About.route) {
                AboutRoute(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TvSettingsRoutes.BASE) {
                val tvViewModel: TvSettingsViewModel = viewModel(
                    factory = TvSettingsViewModelFactory(
                        tvRepository = AppContainer.tvRepository,
                    ),
                )
                TvSettingsRoute(viewModel = tvViewModel)
            }
            composable(AppDestination.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(
                        tokenStore = AppContainer.tmdbTokenStore,
                    ),
                )
                val tvSettingsViewModel: TvSettingsViewModel = viewModel(
                    factory = TvSettingsViewModelFactory(
                        tvRepository = AppContainer.tvRepository,
                    ),
                )
                SettingsRoute(
                    onNavigateToHome = {
                        navController.navigate(AppDestination.Home.route) {
                            popUpTo(AppDestination.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    viewModel = settingsViewModel,
                    tvSettingsViewModel = tvSettingsViewModel,
                )
            }
            composable(
                route = DetailRoutes.BASE,
                arguments = listOf(navArgument("movieId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getInt("movieId") ?: return@composable
                val viewModel: com.kastlg.app.presentation.detail.MovieDetailViewModel =
                    viewModel(
                        factory = MovieDetailViewModelFactory(
                            movieId = movieId,
                            getMovieDetail = AppContainer.getMovieDetail,
                            favoriteRepository = AppContainer.favoriteRepository,
                            historyRepository = AppContainer.historyRepository,
                            tvRepository = AppContainer.tvRepository,
                        ),
                    )

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is com.kastlg.app.presentation.detail.MovieDetailViewModel.NavigationEvent.NavigateToTvSettings -> {
                                navController.navigate(TvSettingsRoutes.BASE) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }

                MovieDetailRoute(
                    onBack = { navController.popBackStack() },
                    onWatchOnTv = { viewModel.watchOnTv() },
                    viewModel = viewModel,
                )
            }
            composable(
                route = TvShowDetailRoutes.BASE,
                arguments = listOf(navArgument("tvShowId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val tvShowId = backStackEntry.arguments?.getInt("tvShowId") ?: return@composable
                val viewModel: com.kastlg.app.presentation.tvdetail.TvShowDetailViewModel =
                    viewModel(
                        factory = TvShowDetailViewModelFactory(
                            tvShowId = tvShowId,
                            getTvShowDetail = AppContainer.getTvShowDetail,
                            getTvSeason = AppContainer.getTvSeason,
                            tvRepository = AppContainer.tvRepository,
                        ),
                    )
                TvShowDetailRoute(
                    onBack = { navController.popBackStack() },
                    onWatchOnTv = { viewModel.watchOnTv() },
                    onSeasonSelected = { seasonNumber -> viewModel.selectSeason(seasonNumber) },
                    onEpisodeSelected = { episode -> viewModel.selectEpisode(episode) },
                    viewModel = viewModel,
                )
            }
        }
    }
}
