package com.void.desktop.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel

@Composable
fun VideoPlayerScreen(
    streamUrl: String,
    title: String,
    onBack: () -> Unit,
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreen: Boolean = false
) {
    var vlcState by remember { mutableStateOf<VlcState>(VlcState.Loading) }
    var mediaPlayerFactory by remember { mutableStateOf<MediaPlayerFactory?>(null) }
    var mediaPlayer by remember { mutableStateOf<EmbeddedMediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0L) }
    var isStretched by remember { mutableStateOf(false) }  // Stretch 4:3 → 16:9

    // Controls visibility
    var showControls by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun resetHideTimer() {
        showControls = true
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(3500)
            if (isPlaying) showControls = false
        }
    }

    val onMouseActivity = rememberUpdatedState(::resetHideTimer)

    // Initialize VLC
    LaunchedEffect(Unit) {
        try {
            val found = NativeDiscovery().discover()
            if (!found) {
                vlcState = VlcState.NotFound
                return@LaunchedEffect
            }
            val factory = MediaPlayerFactory(
                "--no-video-title-show",
                "--quiet",
                "--avcodec-hw=any"        // enable hardware decoding on Windows
            )
            val player = factory.mediaPlayers().newEmbeddedMediaPlayer()
            mediaPlayerFactory = factory
            mediaPlayer = player
            vlcState = VlcState.Ready
        } catch (e: Exception) {
            vlcState = VlcState.NotFound
        }
    }

    // Poll playback position + duration
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(500)
            mediaPlayer?.let { p ->
                position = p.status().position()
                val len = p.status().length()
                if (len > 0) duration = len
            }
        }
    }

    // Apply stretch toggle whenever it changes
    LaunchedEffect(isStretched) {
        val p = mediaPlayer ?: return@LaunchedEffect
        if (isStretched) {
            // Force 16:9 aspect ratio (stretches 4:3 content to fill wide screen)
            p.video().setAspectRatio("16:9")
            p.video().setScale(0f)
        } else {
            // Restore original aspect ratio, auto-scale to fit window
            p.video().setAspectRatio(null)
            p.video().setScale(0f)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.controls()?.stop()
                mediaPlayer?.release()
                mediaPlayerFactory?.release()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Video area ──────────────────────────────────────────────────────
        when (vlcState) {
            VlcState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Initializing player…", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            VlcState.NotFound -> {
                VlcNotFoundContent(streamUrl = streamUrl, onBack = onBack)
            }

            VlcState.Ready -> {
                val factory = mediaPlayerFactory
                val player = mediaPlayer
                if (factory != null && player != null) {
                    val videoCanvas = remember {
                        Canvas().apply {
                            background = java.awt.Color.BLACK
                            // Notify VLC to rescale when canvas resizes
                            addComponentListener(object : ComponentAdapter() {
                                override fun componentResized(e: ComponentEvent) {
                                    player.video().setScale(0f)
                                }
                            })
                        }
                    }

                    val videoPanel = remember {
                        JPanel(BorderLayout()).apply {
                            background = java.awt.Color.BLACK
                            add(videoCanvas, BorderLayout.CENTER)
                            addMouseMotionListener(object : MouseMotionAdapter() {
                                override fun mouseMoved(e: java.awt.event.MouseEvent) {
                                    onMouseActivity.value()
                                }
                                override fun mouseDragged(e: java.awt.event.MouseEvent) {
                                    onMouseActivity.value()
                                }
                            })
                            addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                                    onMouseActivity.value()
                                }
                            })
                        }
                    }

                    LaunchedEffect(streamUrl) {
                        try {
                            val surface = factory.videoSurfaces().newVideoSurface(videoCanvas)
                            player.videoSurface().set(surface)
                            player.media().play(streamUrl)
                            isPlaying = true
                            // Wait for VLC to start rendering, then set auto-scale
                            delay(300)
                            player.video().setScale(0f)          // auto-fit to canvas
                            player.video().setAspectRatio(null)  // keep original AR
                            resetHideTimer()
                        } catch (_: Exception) {
                            vlcState = VlcState.NotFound
                        }
                    }

                    SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory = { videoPanel }
                    )
                }
            }
        }

        // ── Top overlay: Back + Fullscreen (always visible) ────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            // Fullscreen button (always top-right)
            if (onFullscreenToggle != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    IconButton(onClick = onFullscreenToggle) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // ── Bottom controls overlay (auto-hide) ────────────────────────────
        AnimatedVisibility(
            visible = showControls && vlcState == VlcState.Ready,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Time display
                if (duration > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMillis((position * duration).toLong()),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = formatMillis(duration),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Seek bar
                Slider(
                    value = position,
                    onValueChange = { newPos ->
                        position = newPos
                        mediaPlayer?.controls()?.setPosition(newPos)
                        resetHideTimer()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    PlayerIconButton(onClick = {
                        mediaPlayer?.controls()?.skipTime(-10_000)
                        resetHideTimer()
                    }) { Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White) }

                    // Play / Pause
                    PlayerIconButton(onClick = {
                        val p = mediaPlayer ?: return@PlayerIconButton
                        if (p.status().isPlaying) {
                            p.controls().pause(); isPlaying = false
                        } else {
                            p.controls().play(); isPlaying = true
                        }
                        resetHideTimer()
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Forward 10s
                    PlayerIconButton(onClick = {
                        mediaPlayer?.controls()?.skipTime(10_000)
                        resetHideTimer()
                    }) { Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White) }

                    Spacer(Modifier.weight(1f))

                    // Stretch toggle button
                    PlayerToggleButton(
                        active = isStretched,
                        label = "Stretch",
                        onClick = {
                            isStretched = !isStretched
                            resetHideTimer()
                        }
                    )

                    Spacer(Modifier.width(8.dp))

                    // Mute
                    PlayerIconButton(onClick = {
                        mediaPlayer?.audio()?.mute()
                        isMuted = !isMuted
                        resetHideTimer()
                    }) {
                        Icon(
                            if (isMuted) Icons.AutoMirrored.Filled.VolumeMute
                            else Icons.AutoMirrored.Filled.VolumeUp,
                            if (isMuted) "Unmute" else "Mute",
                            tint = Color.White
                        )
                    }

                    // Stop & back
                    PlayerIconButton(onClick = {
                        mediaPlayer?.controls()?.stop()
                        isPlaying = false
                        onBack()
                    }) { Icon(Icons.Default.Stop, "Stop", tint = Color.White) }
                }
            }
        }
    }
}

/** Format milliseconds as h:mm:ss or mm:ss */
private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

@Composable
private fun PlayerIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(onClick = onClick) { content() }
}

/** A text button that shows highlighted when active (for toggles like Stretch). */
@Composable
private fun PlayerToggleButton(
    active: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val bg = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else Color.White

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun VlcNotFoundContent(streamUrl: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "VLC Media Player not found",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Install VLC from videolan.org to enable playback",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(6.dp))
                Text("Go Back")
            }
            Button(onClick = {
                val vlcPaths = listOf(
                    "C:\\Program Files\\VideoLAN\\VLC\\vlc.exe",
                    "C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe"
                )
                val vlcExe = vlcPaths.firstOrNull { java.io.File(it).exists() }
                try {
                    if (vlcExe != null) {
                        ProcessBuilder(vlcExe, streamUrl).start()
                    } else {
                        ProcessBuilder("vlc", streamUrl).start()
                    }
                } catch (_: Exception) {}
            }) {
                Icon(Icons.Default.PlayCircle, null)
                Spacer(Modifier.width(6.dp))
                Text("Open in VLC")
            }
        }
    }
}

private sealed class VlcState {
    object Loading : VlcState()
    object NotFound : VlcState()
    object Ready : VlcState()
}
