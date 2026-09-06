package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.downloads.DownloadsViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.preview.PreviewViewModel
import com.example.ui.preview.ReelPreviewScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import java.net.URLDecoder

data class NavigationTabItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    sharedUrl: String? = null
) {
    val homeViewModel: HomeViewModel = viewModel()
    val downloadsViewModel: DownloadsViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val previewViewModel: PreviewViewModel = viewModel()

    val downloadsUiState by downloadsViewModel.uiState.collectAsStateWithLifecycle()
    val activeDownloadsCount = downloadsUiState.activeDownloads.size

    val tabs = listOf(
        NavigationTabItem(Screen.Home.route, "Home", Icons.Default.Home),
        NavigationTabItem(Screen.Downloads.route, "Downloads", Icons.Default.DownloadDone, badgeCount = activeDownloadsCount),
        NavigationTabItem(Screen.History.route, "History", Icons.Default.History),
        NavigationTabItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    // Handle shared url from external intents (Android Sharesheet)
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            navController.navigate(Screen.Preview.createRoute(sharedUrl)) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isExpandedWidth = configuration.screenWidthDp >= 600
    val isTopLevelDestination = tabs.any { it.route == currentDestination }

    if (isExpandedWidth && isTopLevelDestination) {
        // Canonical Tablet / Wide Layout: NavigationRail
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .testTag("app_navigation_rail")
            ) {
                tabs.forEach { item ->
                    val selected = currentDestination == item.route
                    NavigationRailItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (item.badgeCount > 0) {
                                BadgedBox(badge = { Badge { Text("${item.badgeCount}") } }) {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        },
                        label = { Text(item.title) }
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                NavContent(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    downloadsViewModel = downloadsViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel,
                    previewViewModel = previewViewModel
                )
            }
        }
    } else {
        // Mobile Layout: Bottom Navigation
        Scaffold(
            bottomBar = {
                if (isTopLevelDestination) {
                    NavigationBar(
                        modifier = Modifier.testTag("app_bottom_navigation")
                    ) {
                        tabs.forEach { item ->
                            val selected = currentDestination == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (item.badgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text("${item.badgeCount}") } }) {
                                            Icon(item.icon, contentDescription = item.title)
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavContent(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    downloadsViewModel = downloadsViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel,
                    previewViewModel = previewViewModel
                )
            }
        }
    }
}

@Composable
fun NavContent(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    downloadsViewModel: DownloadsViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    previewViewModel: PreviewViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToPreview = { url ->
                    navController.navigate(Screen.Preview.createRoute(url))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(viewModel = downloadsViewModel)
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateToReel = { url ->
                    navController.navigate(Screen.Preview.createRoute(url))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }

        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
            val decodedUrl = try {
                URLDecoder.decode(encodedUrl, "UTF-8")
            } catch (_: Exception) {
                encodedUrl
            }

            ReelPreviewScreen(
                url = decodedUrl,
                viewModel = previewViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
