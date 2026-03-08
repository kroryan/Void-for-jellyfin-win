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
import uk.co.caprica.vlcj.player.base.MediaPlayer as VlcMediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel
import javax.swing.SwingUtilities

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

    // Stretch toggle — forces video to fill window at 16:9 regardless of native AR
    var isStretched by remember { mutableStateOf(false) }

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

    // ── VLC initialisation ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        try {
            val found = NativeDiscovery().discover()
            if (!found) { vlcState = VlcState.NotFound; return@LaunchedEffect }

            val factory = MediaPlayerFactory("--no-video-title-show", "--quiet")
            val player  = factory.mediaPlayers().newEmbeddedMediaPlayer()

            // ── Register event listener BEFORE starting playback ─────────────
            // videoOutput fires on the VLC internal thread when the vout is
            // initialised.  That is the ONLY reliable moment to call setScale.
            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {

                override fun videoOutput(mp: VlcMediaPlayer, newCount: Int) {
                    if (newCount > 0) {
                        // Scale 0 = auto-fit to the canvas (no 1:1 pixel mapping)
                        mp.video().setScale(0f)
                        mp.video().setAspectRatio(null)   // keep native AR
                    }
                }

                // playing() fires after the first frame — apply scale again as
                // a safety net in case videoOutput fired before the surface was
                // fully attached.
                override fun playing(mp: VlcMediaPlayer) {
                    mp.video().setScale(0f)
                }
            })

            mediaPlayerFactory = factory
            mediaPlayer        = player
            vlcState           = VlcState.Ready
        } catch (e: Exception) {
            vlcState = VlcState.NotFound
        }
    }

    // ── Stretch toggle: re-apply aspect ratio whenever it changes ─────────────
    // This runs on the Compose main thread; libvlc calls are thread-safe.
    LaunchedEffect(isStretched, mediaPlayer) {
        val p = mediaPlayer ?: return@LaunchedEffect
        if (p.status().isPlayable) {
            if (isStretched) {
                p.video().setAspectRatio("16:9")
            } else {
                p.video().setAspectRatio(null)
            }
            p.video().setScale(0f)
        }
    }

    // ── Position/duration polling ─────────────────────────────────────────────
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

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.controls()?.stop()
                mediaPlayer?.release()
                mediaPlayerFactory?.release()
            } catch (_: Exception) {}
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Video area ────────────────────────────────────────────────────────
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

            VlcState.NotFound -> VlcNotFoundContent(streamUrl, onBack)

            VlcState.Ready -> {
                val factory = mediaPlayerFactory
                val player  = mediaPlayer
                if (factory != null && player != null) {

                    // The AWT Canvas that VLC renders into.
                    // BorderLayout.CENTER inside the JPanel ensures it FILLS
                    // the panel — no fixed size, no preferred size overrides.
                    val videoCanvas = remember {
                        Canvas().apply {
                            background = java.awt.Color.BLACK
                        }
                    }

                    // JPanel acts as the SwingPanel host; Canvas fills it.
                    val videoPanel = remember {
                        JPanel(BorderLayout()).apply {
                            background = java.awt.Color.BLACK
                            add(videoCanvas, BorderLayout.CENTER)

                            addMouseMotionListener(object : MouseMotionAdapter() {
                                override fun mouseMoved(e: java.awt.event.MouseEvent)   { onMouseActivity.value() }
                                override fun mouseDragged(e: java.awt.event.MouseEvent) { onMouseActivity.value() }
                            })
                            addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: java.awt.event.MouseEvent) { onMouseActivity.value() }
                            })
                        }
                    }

                    // Attach surface + start playback once
                    val surfaceAttached = remember { AtomicBoolean(false) }
                    LaunchedEffect(streamUrl) {
                        if (surfaceAttached.compareAndSet(false, true)) {
                            // Attach AFTER the JPanel/Canvas are realised by Swing
                            SwingUtilities.invokeLater {
                                try {
                                    val surface = factory.videoSurfaces().newVideoSurface(videoCanvas)
                                    player.videoSurface().set(surface)
                                    player.media().play(streamUrl)
                                } catch (_: Exception) {
                                    vlcState = VlcState.NotFound
                                }
                            }
                            isPlaying = true
                            resetHideTimer()
                        }
                    }

                    // SwingPanel fills the full Box area.
                    // fillMaxSize() here maps to the full pixel area of the parent Box.
                    SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory  = { videoPanel }
                    )
                }
            }
        }

        // ── Always-visible top bar: Back ← … → Fullscreen ────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            CircleIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            if (onFullscreenToggle != null) {
                CircleIconButton(onClick = onFullscreenToggle) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }

        // ── Auto-hide bottom controls ─────────────────────────────────────────
        AnimatedVisibility(
            visible  = showControls && vlcState == VlcState.Ready,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title
                Text(
                    text  = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Elapsed / total time
                if (duration > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatMillis((position * duration).toLong()),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            formatMillis(duration),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Seek bar
                Slider(
                    value       = position,
                    onValueChange = { newPos ->
                        position = newPos
                        mediaPlayer?.controls()?.setPosition(newPos)
                        resetHideTimer()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor        = Color.White,
                        activeTrackColor  = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ─ Rewind 10 s
                    PlayerIconBtn(onClick = {
                        mediaPlayer?.controls()?.skipTime(-10_000); resetHideTimer()
                    }) { Icon(Icons.Default.Replay10, "–10 s", tint = Color.White) }

                    // ─ Play / Pause
                    PlayerIconBtn(onClick = {
                        val p = mediaPlayer ?: return@PlayerIconBtn
                        if (p.status().isPlaying) { p.controls().pause(); isPlaying = false }
                        else                      { p.controls().play();  isPlaying = true  }
                        resetHideTimer()
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // ─ Forward 10 s
                    PlayerIconBtn(onClick = {
                        mediaPlayer?.controls()?.skipTime(10_000); resetHideTimer()
                    }) { Icon(Icons.Default.Forward10, "+10 s", tint = Color.White) }

                    Spacer(Modifier.weight(1f))

                    // ─ Stretch toggle
                    ToggleChip(
                        label   = "Stretch",
                        active  = isStretched,
                        onClick = { isStretched = !isStretched; resetHideTimer() }
                    )

                    Spacer(Modifier.width(8.dp))

                    // ─ Mute
                    PlayerIconBtn(onClick = {
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

                    // ─ Stop → back
                    PlayerIconBtn(onClick = {
                        mediaPlayer?.controls()?.stop()
                        isPlaying = false
                        onBack()
                    }) { Icon(Icons.Default.Stop, "Stop", tint = Color.White) }
                }
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

private fun formatMillis(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

/** onClick is first so the icon can be the trailing lambda. */
@Composable
private fun PlayerIconBtn(onClick: () -> Unit, icon: @Composable () -> Unit) {
    IconButton(onClick = onClick) { icon() }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f)
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(6.dp),
        color   = bg,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text  = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
        Icon(Icons.Default.Warning, null, tint = Color(0xFFFFD700), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("VLC Media Player not found", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Install VLC from videolan.org to enable playback",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
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
                    if (vlcExe != null) ProcessBuilder(vlcExe, streamUrl).start()
                    else ProcessBuilder("vlc", streamUrl).start()
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
    object Loading  : VlcState()
    object NotFound : VlcState()
    object Ready    : VlcState()
}
