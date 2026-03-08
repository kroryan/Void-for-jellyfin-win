package com.void.desktop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoidDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BD0FF),
    onPrimary = Color(0xFF003352),
    primaryContainer = Color(0xFF004A73),
    onPrimaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFFB6C8D8),
    onSecondary = Color(0xFF213240),
    secondaryContainer = Color(0xFF374957),
    onSecondaryContainer = Color(0xFFD2E4F5),
    tertiary = Color(0xFFCBBBE9),
    onTertiary = Color(0xFF342A4C),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF141719),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF1E2124),
    onSurfaceVariant = Color(0xFFC2C6CE),
    outline = Color(0xFF8C9099),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun VoidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoidDarkColorScheme,
        content = content
    )
}
