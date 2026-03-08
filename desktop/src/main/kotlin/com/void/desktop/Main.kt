package com.void.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.void.desktop.data.api.ApiClient
import com.void.desktop.ui.App
import com.void.desktop.ui.components.CustomTitleBar
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/** Creates a simple "V" icon for the app in the taskbar. */
fun createAppIcon(): BufferedImage {
    val size = 128
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    // Background
    g.color = java.awt.Color(15, 17, 21)
    g.fillRoundRect(0, 0, size, size, 24, 24)
    // "V" letter in brand color
    g.color = java.awt.Color(155, 208, 255)
    g.font = java.awt.Font("Arial", java.awt.Font.BOLD, 82)
    val fm = g.fontMetrics
    val x = (size - fm.stringWidth("V")) / 2
    val y = (size + fm.ascent - fm.descent) / 2
    g.drawString("V", x, y)
    g.dispose()
    return img
}

fun main() {
    // Configure Coil 3 image loader with OkHttp
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { ApiClient.okHttpClient }))
            }
            .build()
    }

    application {
        val windowState = rememberWindowState(
            size = DpSize(1280.dp, 800.dp),
            placement = WindowPlacement.Floating,
            position = WindowPosition(alignment = androidx.compose.ui.Alignment.Center)
        )

        var isFullscreen by remember { mutableStateOf(false) }

        fun toggleFullscreen() {
            isFullscreen = !isFullscreen
            windowState.placement =
                if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
        }

        val isMaximized = windowState.placement == WindowPlacement.Maximized

        Window(
            onCloseRequest = ::exitApplication,
            title = "Void for Jellyfin",
            state = windowState,
            undecorated = true,  // Custom dark title bar
            onKeyEvent = { keyEvent ->
                when {
                    keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F11 -> {
                        toggleFullscreen(); true
                    }
                    keyEvent.type == KeyEventType.KeyDown &&
                            keyEvent.key == Key.Escape && isFullscreen -> {
                        toggleFullscreen(); true
                    }
                    else -> false
                }
            }
        ) {
            // Set taskbar icon
            LaunchedEffect(Unit) {
                window.iconImages = listOf(createAppIcon())
            }

            Column(Modifier.fillMaxSize()) {
                // Custom title bar (hidden in fullscreen)
                if (!isFullscreen) {
                    CustomTitleBar(
                        title = "Void for Jellyfin",
                        window = window,
                        isMaximized = isMaximized,
                        onMinimize = { windowState.isMinimized = true },
                        onMaximizeToggle = {
                            windowState.placement =
                                if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                        },
                        onClose = { exitApplication() }
                    )
                }

                App(
                    isFullscreen = isFullscreen,
                    onFullscreenToggle = { toggleFullscreen() }
                )
            }
        }
    }
}
