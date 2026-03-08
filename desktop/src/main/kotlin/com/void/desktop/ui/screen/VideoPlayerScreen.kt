package com.void.desktop.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import java.awt.Color as AwtColor
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel

enum class AspectRatio(val displayName: String, val vlcArgs: List<String>) {
    ORIGINAL("Original", emptyList()),
    FIT("Fit", listOf("--no-keep-aspect-ratio")),
    FILL("Fill", listOf("--no-keep-aspect-ratio", "--no-video-title-show")),
    STRETCH("Stretch", listOf("--no-keep-aspect-ratio", "--adaptive", "--video-filter=distort"))
}

enum class PlaybackSpeed(val multiplier: Float, val displayName: String) {
    SLOW_0_25(0.25f, "0.25x"),
    SLOW_0_5(0.5f, "0.5x"),
    SLOW_0_75(0.75f, "0.75x"),
    NORMAL_1_0(1.0f, "1.0x"),
    FAST_1_25(1.25f, "1.25x"),
    FAST_1_5(1.5f, "1.5x"),
    FAST_1_75(1.75f, "1.75x"),
    FAST_2_0(2.0f, "2.0x")
}

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

    // Settings
    var currentAspectRatio by remember { mutableStateOf(AspectRatio.FIT) }
    var currentSpeed by remember { mutableStateOf(PlaybackSpeed.NORMAL_1_0) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var currentStreamUrl by remember { mutableStateOf(streamUrl) }

    // Controls visibility
    var showControls by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun resetHideTimer() {
        showControls = true
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(4000)
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

            val args = mutableListOf("--no-video-title-show", "--quiet", "--no-xlib")
            args.addAll(currentAspectRatio.vlcArgs)

            val factory = MediaPlayerFactory(*args.toTypedArray())
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
            mediaPlayer?.let { mp ->
                position = mp.status().position()
                val lengthMs = mp.status().length()
                duration = if (lengthMs > 0) lengthMs else duration
            }
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
        when (val state = vlcState) {
            VlcState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading player...", color = Color.White.copy(alpha = 0.6f))
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
                    // Canvas para renderizar video
                    val videoCanvas = remember {
                        Canvas().apply { background = AwtColor.BLACK }
                    }

                    // JPanel que contiene el Canvas
                    val videoPanel = remember {
                        JPanel(BorderLayout()).apply {
                            background = AwtColor.BLACK
                            add(videoCanvas, BorderLayout.CENTER)

                            // Detectar movimientos de ratón
                            addMouseMotionListener(object : MouseMotionAdapter() {
                                override fun mouseMoved(e: java.awt.event.MouseEvent) {
                                    onMouseActivity.value()
                                }
                            })
                            addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                                    // Toggle play/pause on click
                                    if (isPlaying) {
                                        player.controls().pause()
                                        isPlaying = false
                                    } else {
                                        player.controls().play()
                                        isPlaying = true
                                    }
                                    resetHideTimer()
                                }
                            })
                        }
                    }

                    // Iniciar reproducción
                    LaunchedEffect(streamUrl) {
                        try {
                            val surface = factory.videoSurfaces().newVideoSurface(videoCanvas)
                            player.videoSurface().set(surface)
                            player.media().play(streamUrl)
                            player.controls().setRate(currentSpeed.multiplier)
                            isPlaying = true
                            resetHideTimer()
                        } catch (e: Exception) {
                            vlcState = VlcState.NotFound
                        }
                    }

                    // Aplicar cambios de velocidad
                    LaunchedEffect(currentSpeed) {
                        player.controls().setRate(currentSpeed.multiplier)
                    }

                    // Área de video
                    SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory = { videoPanel }
                    )
                }
            }
        }

        // ── Back button (top-left) ───────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // ── Fullscreen button (top-right) ──────────────────────────────────
        if (onFullscreenToggle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onFullscreenToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }

        // ── Controls overlay ─────────────────────────────────────────────
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
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f),
                                Color.Black
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                // Title and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${formatTime((position * 100).toLong())} / ${formatTime(duration)}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(8.dp))

                // Main controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Playback controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        ControlButton(Icons.Default.Replay10, "Rewind 10s") {
                            mediaPlayer?.controls()?.skipTime(-10_000)
                            resetHideTimer()
                        }

                        // Play/Pause
                        ControlButton(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play"
                        ) {
                            val p = mediaPlayer ?: return@ControlButton
                            if (p.status().isPlaying) {
                                p.controls().pause()
                                isPlaying = false
                            } else {
                                p.controls().play()
                                isPlaying = true
                            }
                            resetHideTimer()
                        }

                        // Forward 10s
                        ControlButton(Icons.Default.Forward10, "Forward 10s") {
                            mediaPlayer?.controls()?.skipTime(10_000)
                            resetHideTimer()
                        }

                        // Stop
                        ControlButton(Icons.Default.Stop, "Stop") {
                            mediaPlayer?.controls()?.stop()
                            isPlaying = false
                            onBack()
                        }
                    }

                    // Center: Speed control
                    Box {
                        ControlButton(
                            Icons.Default.Speed,
                            "Speed: ${currentSpeed.displayName}"
                        ) {
                            showSpeedMenu = true
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            PlaybackSpeed.values().forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            speed.displayName,
                                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        currentSpeed = speed
                                        showSpeedMenu = false
                                        resetHideTimer()
                                    }
                                )
                            }
                        }
                    }

                    // Right: Settings controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect ratio
                        Box {
                            ControlButton(
                                Icons.Default.AspectRatio,
                                "Aspect Ratio: ${currentAspectRatio.displayName}"
                            ) {
                                showAspectRatioMenu = true
                            }

                            DropdownMenu(
                                expanded = showAspectRatioMenu,
                                onDismissRequest = { showAspectRatioMenu = false }
                            ) {
                                AspectRatio.values().forEach { ratio ->
                                    DropdownMenuItem(
                                    text = {
                                        Text(
                                            ratio.displayName,
                                            fontWeight = if (ratio == currentAspectRatio) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        currentAspectRatio = ratio
                                        showAspectRatioMenu = false
                                        resetHideTimer()
                                        // Restart playback with new aspect ratio
                                        mediaPlayer?.let { mp ->
                                            mp.controls().stop()
                                            mp.media().play(currentStreamUrl)
                                            isPlaying = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                        // Mute
                        ControlButton(
                            if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            if (isMuted) "Unmute" else "Mute"
                        ) {
                            mediaPlayer?.audio()?.mute()
                            isMuted = !isMuted
                            resetHideTimer()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
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
            "Install VLC 64-bit from videolan.org to enable playback",
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

private fun formatTime(seconds: Long): String {
    val totalSeconds = (seconds / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}
