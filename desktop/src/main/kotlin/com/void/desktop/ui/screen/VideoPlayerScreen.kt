package com.void.desktop.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.BorderLayout
import java.awt.Canvas
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

    // Controls visibility - managed by mouse movement
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

    // Mouse movement callback captured via rememberUpdatedState so the AWT listener
    // always invokes the latest lambda without recreating the panel.
    val onMouseActivity = rememberUpdatedState(::resetHideTimer)

    // Initialize VLC
    LaunchedEffect(Unit) {
        try {
            val found = NativeDiscovery().discover()
            if (!found) {
                vlcState = VlcState.NotFound
                return@LaunchedEffect
            }
            val factory = MediaPlayerFactory("--no-video-title-show", "--quiet", "--no-xlib")
            val player = factory.mediaPlayers().newEmbeddedMediaPlayer()
            mediaPlayerFactory = factory
            mediaPlayer = player
            vlcState = VlcState.Ready
        } catch (e: Exception) {
            vlcState = VlcState.NotFound
        }
    }

    // Poll playback position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(500)
            position = mediaPlayer?.status()?.position() ?: 0f
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
        when (val state = vlcState) {
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
                        Canvas().apply { background = java.awt.Color.BLACK }
                    }
                    // Wrap the AWT Canvas in a JPanel so SwingPanel can use it
                    val videoPanel = remember(onMouseActivity) {
                        JPanel(BorderLayout()).apply {
                            background = java.awt.Color.BLACK
                            add(videoCanvas, BorderLayout.CENTER)
                            // Detect mouse movement to show controls
                            addMouseMotionListener(object : MouseMotionAdapter() {
                                override fun mouseMoved(e: java.awt.event.MouseEvent) {
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

                    // Start playback once
                    LaunchedEffect(streamUrl) {
                        try {
                            val surface = factory.videoSurfaces().newVideoSurface(videoCanvas)
                            player.videoSurface().set(surface)
                            player.media().play(streamUrl)
                            isPlaying = true
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

        // ── Always-visible back button ─────────────────────────────────────
        // Even when controls are hidden, the back button stays at top-left.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // ── Always-visible fullscreen button (top-right) ───────────────────
        if (onFullscreenToggle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
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

        // ── Fade-in/out controls overlay ───────────────────────────────────
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

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

                // Playback controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Rewind 10s
                    PlayerIconButton(
                        icon = { Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White) },
                        onClick = {
                            mediaPlayer?.controls()?.skipTime(-10_000)
                            resetHideTimer()
                        }
                    )

                    // Play / Pause
                    PlayerIconButton(
                        icon = {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        onClick = {
                            val p = mediaPlayer ?: return@PlayerIconButton
                            if (p.status().isPlaying) {
                                p.controls().pause(); isPlaying = false
                            } else {
                                p.controls().play(); isPlaying = true
                            }
                            resetHideTimer()
                        }
                    )

                    // Forward 10s
                    PlayerIconButton(
                        icon = { Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White) },
                        onClick = {
                            mediaPlayer?.controls()?.skipTime(10_000)
                            resetHideTimer()
                        }
                    )

                    Spacer(Modifier.weight(1f))

                    // Mute
                    PlayerIconButton(
                        icon = {
                            Icon(
                                if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        },
                        onClick = {
                            mediaPlayer?.audio()?.mute()
                            isMuted = !isMuted
                            resetHideTimer()
                        }
                    )

                    // Stop
                    PlayerIconButton(
                        icon = { Icon(Icons.Default.Stop, "Stop", tint = Color.White) },
                        onClick = {
                            mediaPlayer?.controls()?.stop()
                            isPlaying = false
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) { icon() }
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
                // Try to open with VLC from common install paths
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
