package com.void.desktop.ui

import androidx.compose.runtime.*
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.data.storage.PreferencesStorage
import com.void.desktop.ui.screen.*
import com.void.desktop.ui.theme.VoidTheme

private sealed class Screen {
    object ServerSetup : Screen()
    data class Login(val serverUrl: String) : Screen()
    data class Home(val prefs: AppPreferences) : Screen()
    data class Library(val library: BaseItemDto, val prefs: AppPreferences) : Screen()
    data class MediaDetail(val itemId: String, val prefs: AppPreferences) : Screen()
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

        fun push(screen: Screen) = navStack.add(screen)
        fun pop() { if (navStack.size > 1) navStack.removeLastOrNull() }

        when (val screen = currentScreen) {
            is Screen.ServerSetup -> ServerSetupScreen(
                onServerConnected = { url -> push(Screen.Login(url)) }
            )

            is Screen.Login -> LoginScreen(
                serverUrl = screen.serverUrl,
                deviceId = PreferencesStorage.load().deviceId,
                onLoginSuccess = { prefs ->
                    navStack.clear()
                    navStack.add(Screen.Home(prefs))
                },
                onChangeServer = { pop() }
            )

            is Screen.Home -> HomeScreen(
                prefs = screen.prefs,
                onItemClick = { item -> push(Screen.MediaDetail(item.id, screen.prefs)) },
                onLibraryClick = { library -> push(Screen.Library(library, screen.prefs)) },
                onLogout = {
                    PreferencesStorage.clear()
                    navStack.clear()
                    navStack.add(Screen.ServerSetup)
                }
            )

            is Screen.Library -> LibraryScreen(
                library = screen.library,
                prefs = screen.prefs,
                onItemClick = { item -> push(Screen.MediaDetail(item.id, screen.prefs)) },
                onBack = { pop() }
            )

            is Screen.MediaDetail -> MediaDetailScreen(
                itemId = screen.itemId,
                prefs = screen.prefs,
                onPlay = { streamUrl, title -> push(Screen.Player(streamUrl, title, screen.prefs)) },
                onBack = { pop() }
            )

            is Screen.Player -> VideoPlayerScreen(
                streamUrl = screen.streamUrl,
                title = screen.title,
                onBack = { pop() },
                onFullscreenToggle = onFullscreenToggle,
                isFullscreen = isFullscreen
            )
        }
    }
}
