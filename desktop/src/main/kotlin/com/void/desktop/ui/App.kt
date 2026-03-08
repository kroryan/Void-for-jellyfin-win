package com.void.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.data.storage.PreferencesStorage
import com.void.desktop.ui.screen.*

// ── Top-level app navigation ─────────────────────────────────────────────────

private sealed class AppScreen {
    object ServerSetup : AppScreen()
    data class Login(val serverUrl: String) : AppScreen()
    data class Main(val prefs: AppPreferences) : AppScreen()
    data class Player(val streamUrl: String, val title: String, val prefs: AppPreferences) : AppScreen()
}

// ── Pages within the main authenticated area ──────────────────────────────────

sealed class TabPage {
    object Home     : TabPage()
    object Search   : TabPage()
    object Favorites : TabPage()
    object Settings : TabPage()
    data class Library(val library: BaseItemDto) : TabPage()
    data class MediaDetail(val itemId: String) : TabPage()
}

// ── Navigation rail tab descriptors ──────────────────────────────────────────

private data class NavTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

private val NAV_TABS = listOf(
    NavTab("Home",      Icons.Default.Home,           Icons.Default.Home),
    NavTab("Search",    Icons.Default.Search,          Icons.Default.Search),
    NavTab("Favorites", Icons.Default.FavoriteBorder,  Icons.Default.Favorite),
    NavTab("Settings",  Icons.Default.Settings,         Icons.Default.Settings)
)

// ── App root ─────────────────────────────────────────────────────────────────

@Composable
fun App(
    isFullscreen: Boolean = false,
    onFullscreenToggle: () -> Unit = {}
) {
    // Scale the UI by 28% so everything appears larger on desktop monitors
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * 1.28f,
            fontScale = baseDensity.fontScale
        )
    ) {

    val initialPrefs = remember { PreferencesStorage.load() }
    val initialScreen: AppScreen = remember {
        when {
            initialPrefs.accessToken.isNotBlank() && initialPrefs.serverUrl.isNotBlank() ->
                AppScreen.Main(initialPrefs)
            initialPrefs.serverUrl.isNotBlank() ->
                AppScreen.Login(initialPrefs.serverUrl)
            else ->
                AppScreen.ServerSetup
        }
    }

    val appStack = remember { mutableStateListOf<AppScreen>(initialScreen) }
    val currentAppScreen = appStack.last()

    fun pushApp(screen: AppScreen) = appStack.add(screen)
    fun popApp() { if (appStack.size > 1) appStack.removeLastOrNull() }

    when (val screen = currentAppScreen) {
        is AppScreen.ServerSetup -> ServerSetupScreen(
            onServerConnected = { url -> pushApp(AppScreen.Login(url)) }
        )

        is AppScreen.Login -> LoginScreen(
            serverUrl = screen.serverUrl,
            deviceId = PreferencesStorage.load().deviceId,
            onLoginSuccess = { prefs ->
                appStack.clear()
                appStack.add(AppScreen.Main(prefs))
            },
            onChangeServer = { popApp() }
        )

        is AppScreen.Main -> MainScreen(
            prefs = screen.prefs,
            isFullscreen = isFullscreen,
            onFullscreenToggle = onFullscreenToggle,
            onLogout = {
                PreferencesStorage.clear()
                appStack.clear()
                appStack.add(AppScreen.ServerSetup)
            },
            onNavigateToPlayer = { url, title ->
                pushApp(AppScreen.Player(url, title, screen.prefs))
            }
        )

        is AppScreen.Player -> VideoPlayerScreen(
            streamUrl = screen.streamUrl,
            title = screen.title,
            onBack = { popApp() },
            onFullscreenToggle = onFullscreenToggle,
            isFullscreen = isFullscreen
        )
    }

    } // end CompositionLocalProvider
}

// ── Main authenticated layout with navigation rail ────────────────────────────

@Composable
private fun MainScreen(
    prefs: AppPreferences,
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToPlayer: (url: String, title: String) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Each tab has its own independent page stack
    val tabStacks: List<MutableList<TabPage>> = remember {
        listOf(
            mutableStateListOf(TabPage.Home),
            mutableStateListOf(TabPage.Search),
            mutableStateListOf(TabPage.Favorites),
            mutableStateListOf(TabPage.Settings)
        )
    }

    val currentStack = tabStacks[selectedTabIndex]
    val currentPage = currentStack.last()

    fun pushPage(page: TabPage) = currentStack.add(page)
    fun popPage() { if (currentStack.size > 1) currentStack.removeLastOrNull() }

    Row(modifier = Modifier.fillMaxSize()) {
        // ── Navigation Rail ───────────────────────────────────────────────────
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Spacer(Modifier.weight(1f))
            NAV_TABS.forEachIndexed { index, tab ->
                NavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == index) tab.selectedIcon else tab.icon,
                            contentDescription = tab.label
                        )
                    },
                    label = { Text(tab.label) },
                    selected = selectedTabIndex == index,
                    onClick = {
                        if (selectedTabIndex == index) {
                            // Tap current tab → pop to root
                            while (currentStack.size > 1) currentStack.removeLastOrNull()
                        } else {
                            selectedTabIndex = index
                        }
                    }
                )
            }
            Spacer(Modifier.weight(1f))
        }

        // Thin divider between rail and content
        HorizontalDivider(
            modifier = Modifier.fillMaxHeight().width(androidx.compose.ui.unit.Dp.Hairline),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        // ── Content area ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (val page = currentPage) {
                is TabPage.Home -> HomeScreen(
                    prefs = prefs,
                    onItemClick    = { item    -> pushPage(TabPage.MediaDetail(item.id)) },
                    onLibraryClick = { library -> pushPage(TabPage.Library(library)) }
                )

                is TabPage.Search -> SearchScreen(
                    prefs = prefs,
                    onItemClick = { item -> pushPage(TabPage.MediaDetail(item.id)) }
                )

                is TabPage.Favorites -> FavoritesScreen(
                    prefs = prefs,
                    onItemClick = { item -> pushPage(TabPage.MediaDetail(item.id)) }
                )

                is TabPage.Settings -> SettingsScreen(
                    prefs = prefs,
                    onLogout = onLogout
                )

                is TabPage.Library -> LibraryScreen(
                    library = page.library,
                    prefs = prefs,
                    onItemClick = { item -> pushPage(TabPage.MediaDetail(item.id)) },
                    onBack = { popPage() }
                )

                is TabPage.MediaDetail -> MediaDetailScreen(
                    itemId = page.itemId,
                    prefs = prefs,
                    onPlay = { url, title -> onNavigateToPlayer(url, title) },
                    onBack = { popPage() }
                )
            }
        }
    }
}
