package com.void.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.void.desktop.data.api.ApiClient
import com.void.desktop.ui.App
import com.void.desktop.ui.components.CustomTitleBar
import java.awt.image.BufferedImage

/** Loads the real app icon from classpath resources. */
fun loadAppIcon(): BufferedImage? =
    object {}.javaClass.getResourceAsStream("/icon.png")?.use {
        javax.imageio.ImageIO.read(it)
    }

fun main() {
    // Set up error handler to catch initialization errors
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        throwable.printStackTrace()
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Error starting Void for Jellyfin:\n\n${throwable.message}\n\nStack trace:\n${throwable.stackTraceToString()}",
            "Startup Error",
            javax.swing.JOptionPane.ERROR_MESSAGE
        )
        System.exit(1)
    }

    try {
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
                loadAppIcon()?.let { window.iconImages = listOf(it) }
            }

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density * 1.3f, baseDensity.fontScale)
            ) {
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
            } // end CompositionLocalProvider
        }
    }
    } catch (e: Exception) {
        e.printStackTrace()
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Failed to start Void for Jellyfin:\n\n${e.message}\n\n${e.stackTraceToString()}",
            "Fatal Error",
            javax.swing.JOptionPane.ERROR_MESSAGE
        )
        System.exit(1)
    }
}
