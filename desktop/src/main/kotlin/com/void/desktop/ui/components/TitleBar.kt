package com.void.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CustomTitleBar(
    title: String,
    window: java.awt.Window?,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximizeToggle: () -> Unit,
    onClose: () -> Unit
) {
    val appIcon: ImageBitmap? = remember {
        try {
            object {}.javaClass.getResourceAsStream("/icon.png")?.use { stream ->
                loadImageBitmap(stream)
            }
        } catch (_: Exception) { null }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(isMaximized) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!isMaximized && window != null) {
                        window.setLocation(
                            (window.x + dragAmount.x).roundToInt(),
                            (window.y + dragAmount.y).roundToInt()
                        )
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        // Real app icon or fallback
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = "Void",
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "V",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f)
        )

        // Separator line
        Spacer(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )

        // Minimize
        TitleBarButton(
            onClick = onMinimize,
            hoverColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Minimize",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }

        // Maximize / Restore
        TitleBarButton(
            onClick = onMaximizeToggle,
            hoverColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                if (isMaximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                contentDescription = if (isMaximized) "Restore" else "Maximize",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp)
            )
        }

        // Close (red on hover)
        TitleBarButton(
            onClick = onClose,
            hoverColor = Color(0xFFC42B1C)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TitleBarButton(
    onClick: () -> Unit,
    hoverColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .background(if (isHovered) hoverColor else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
