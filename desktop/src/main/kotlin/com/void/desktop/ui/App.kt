package com.void.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.data.storage.PreferencesStorage
import com.void.desktop.ui.screen.*
import com.void.desktop.ui.theme.VoidTheme

private enum class NavDestination {
    HOME, SEARCH, FAVORITES, SETTINGS
}

private sealed class Screen {
    object ServerSetup : Screen()
    data class Login(val serverUrl: String) : Screen()
    data class Home(val prefs: AppPreferences) : Screen()
    data class Search(val prefs: AppPreferences) : Screen()
    data class Favorites(val prefs: AppPreferences) : Screen()
    data class Settings(val prefs: AppPreferences) : Screen()
    data class Library(val library: BaseItemDto, val prefs: AppPreferences) : Screen()
    data class MediaDetail(val itemId: String, val prefs: AppPreferences) : Screen()
    data class SeriesDetail(val seriesId: String, val prefs: AppPreferences) : Screen()
    data class SeasonDetail(val seasonId: String, val seriesName: String, val prefs: AppPreferences) : Screen()
    data class Player(val streamUrl: String, val title: String, val prefs: AppPreferences) : Screen()
}

@Composable
fun App(
    isFullscreen: Boolean = false,
    onFullscreenToggle: () -> Unit = {}
) {
    VoidTheme {
        val initialPrefs = remember { PreferencesStorage.load() }
        val initialScreen: Screen = remember {
            when {
                initialPrefs.accessToken.isNotBlank() && initialPrefs.serverUrl.isNotBlank() ->
                    Screen.Home(initialPrefs)
                initialPrefs.serverUrl.isNotBlank() ->
                    Screen.Login(initialPrefs.serverUrl)
                else ->
                    Screen.ServerSetup
            }
        }

        val navStack = remember { mutableStateListOf<Screen>(initialScreen) }
        val currentScreen = navStack.last()
        var currentDestination by remember { mutableStateOf(NavDestination.HOME) }

        fun push(screen: Screen) = navStack.add(screen)
        fun pop() { if (navStack.size > 1) navStack.removeLastOrNull() }

        fun navigateToDestination(dest: NavDestination, prefs: AppPreferences) {
            currentDestination = dest
            navStack.clear()
            when (dest) {
                NavDestination.HOME -> push(Screen.Home(prefs))
                NavDestination.SEARCH -> push(Screen.Search(prefs))
                NavDestination.FAVORITES -> push(Screen.Favorites(prefs))
                NavDestination.SETTINGS -> push(Screen.Settings(prefs))
            }
        }

        fun getCurrentPrefs(): AppPreferences = when (val screen = currentScreen) {
            is Screen.Home -> screen.prefs
            is Screen.Search -> screen.prefs
            is Screen.Favorites -> screen.prefs
            is Screen.Settings -> screen.prefs
            is Screen.Library -> screen.prefs
            is Screen.MediaDetail -> screen.prefs
            is Screen.SeriesDetail -> screen.prefs
            is Screen.SeasonDetail -> screen.prefs
            is Screen.Player -> screen.prefs
            is Screen.ServerSetup, is Screen.Login -> initialPrefs
        }

        val prefs = getCurrentPrefs()
        val isAuthenticated = prefs.accessToken.isNotBlank() && prefs.serverUrl.isNotBlank()

        if (!isAuthenticated || currentScreen is Screen.Player) {
            // Auth screens or fullscreen player - no nav rail
            when (val screen = currentScreen) {
                is Screen.ServerSetup -> ServerSetupScreen(
                    onServerConnected = { url -> push(Screen.Login(url)) }
                )

                is Screen.Login -> LoginScreen(
                    serverUrl = screen.serverUrl,
                    deviceId = PreferencesStorage.load().deviceId,
                    onLoginSuccess = { newPrefs ->
                        navStack.clear()
                        navStack.add(Screen.Home(newPrefs))
                    },
                    onChangeServer = { pop() }
                )

                is Screen.Player -> VideoPlayerScreen(
                    streamUrl = screen.streamUrl,
                    title = screen.title,
                    onBack = { pop() },
                    onFullscreenToggle = onFullscreenToggle,
                    isFullscreen = isFullscreen,
                    customVlcPath = screen.prefs.customVlcPath
                )

                else -> {}
            }
        } else {
            // Authenticated screens - show nav rail
            Row(Modifier.fillMaxSize()) {
                // Navigation Rail
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationRailItem(
                        selected = currentDestination == NavDestination.HOME,
                        onClick = { navigateToDestination(NavDestination.HOME, prefs) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationRailItem(
                        selected = currentDestination == NavDestination.SEARCH,
                        onClick = { navigateToDestination(NavDestination.SEARCH, prefs) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationRailItem(
                        selected = currentDestination == NavDestination.FAVORITES,
                        onClick = { navigateToDestination(NavDestination.FAVORITES, prefs) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") }
                    )
                    NavigationRailItem(
                        selected = currentDestination == NavDestination.SETTINGS,
                        onClick = { navigateToDestination(NavDestination.SETTINGS, prefs) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }

                // Content
                Box(Modifier.weight(1f)) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(
                            prefs = screen.prefs,
                            onItemClick = { item ->
                                currentDestination = NavDestination.HOME
                                if (item.type == "Series") {
                                    push(Screen.SeriesDetail(item.id, screen.prefs))
                                } else {
                                    push(Screen.MediaDetail(item.id, screen.prefs))
                                }
                            },
                            onLibraryClick = { library -> push(Screen.Library(library, screen.prefs)) }
                        )

                        is Screen.Search -> SearchScreen(
                            prefs = screen.prefs,
                            onItemClick = { item ->
                                if (item.type == "Series") {
                                    push(Screen.SeriesDetail(item.id, screen.prefs))
                                } else {
                                    push(Screen.MediaDetail(item.id, screen.prefs))
                                }
                            }
                        )

                        is Screen.Favorites -> FavoritesScreen(
                            prefs = screen.prefs,
                            onItemClick = { item ->
                                if (item.type == "Series") {
                                    push(Screen.SeriesDetail(item.id, screen.prefs))
                                } else {
                                    push(Screen.MediaDetail(item.id, screen.prefs))
                                }
                            }
                        )

                        is Screen.Settings -> SettingsScreen(
                            prefs = screen.prefs,
                            onLogout = {
                                PreferencesStorage.clear()
                                navStack.clear()
                                navStack.add(Screen.ServerSetup)
                            },
                            onSavePreferences = { newPrefs ->
                                PreferencesStorage.save(newPrefs)
                                // Update the current screen with new prefs
                                navStack[navStack.size - 1] = Screen.Settings(newPrefs)
                            }
                        )

                        is Screen.Library -> LibraryScreen(
                            library = screen.library,
                            prefs = screen.prefs,
                            onItemClick = { item ->
                                if (item.type == "Series") {
                                    push(Screen.SeriesDetail(item.id, screen.prefs))
                                } else {
                                    push(Screen.MediaDetail(item.id, screen.prefs))
                                }
                            },
                            onBack = { pop() }
                        )

                        is Screen.MediaDetail -> MediaDetailScreen(
                            itemId = screen.itemId,
                            prefs = screen.prefs,
                            onPlay = { streamUrl, title -> push(Screen.Player(streamUrl, title, screen.prefs)) },
                            onBack = { pop() }
                        )

                        is Screen.SeriesDetail -> SeriesDetailScreen(
                            seriesId = screen.seriesId,
                            prefs = screen.prefs,
                            onSeasonClick = { seasonId, seasonName ->
                                push(Screen.SeasonDetail(seasonId, seasonName, screen.prefs))
                            },
                            onEpisodeClick = { episodeId ->
                                push(Screen.MediaDetail(episodeId, screen.prefs))
                            },
                            onBack = { pop() }
                        )

                        is Screen.SeasonDetail -> SeasonDetailScreen(
                            seasonId = screen.seasonId,
                            seriesName = screen.seriesName,
                            prefs = screen.prefs,
                            onItemClick = { item ->
                                if (item.type == "Series") {
                                    push(Screen.SeriesDetail(item.id, screen.prefs))
                                } else {
                                    push(Screen.MediaDetail(item.id, screen.prefs))
                                }
                            },
                            onBack = { pop() }
                        )

                        is Screen.ServerSetup, is Screen.Login, is Screen.Player -> {}
                    }
                }
            }
        }
    }
}
