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
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer as VlcMediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

// ─────────────────────────────────────────────────────────────────────────────
//  VideoPlayerScreen
//
//  Uses EmbeddedMediaPlayerComponent — the vlcj high-level API.
//  This JPanel subclass manages the native Canvas/HWND internally and
//  properly embeds in the Compose window on Windows via SwingPanel.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VideoPlayerScreen(
    streamUrl: String,
    title: String,
    onBack: () -> Unit,
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreen: Boolean = false
) {
    var vlcState by remember { mutableStateOf<VlcState>(VlcState.Loading) }

    // The high-level component — is a JPanel, used directly in SwingPanel
    var playerComponent by remember { mutableStateOf<EmbeddedMediaPlayerComponent?>(null) }

    var isPlaying   by remember { mutableStateOf(false) }
    var isMuted     by remember { mutableStateOf(false) }
    var position    by remember { mutableStateOf(0f) }
    var duration    by remember { mutableStateOf(0L) }
    var isStretched by remember { mutableStateOf(false) }

    // Controls auto-hide
    var showControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun resetHideTimer() {
        showControls = true
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(3500)
            if (isPlaying) showControls = false
        }
    }

    // ── Initialise VLC ────────────────────────────────────────────────────────
    // Must run on a background coroutine (NativeDiscovery can block briefly).
    LaunchedEffect(Unit) {
        try {
            val found = NativeDiscovery().discover()
            if (!found) { vlcState = VlcState.NotFound; return@LaunchedEffect }

            // EmbeddedMediaPlayerComponent creates its own factory, Canvas,
            // and video surface.  It is the official vlcj way to embed video
            // in a Swing container and works correctly on Windows.
            val component = EmbeddedMediaPlayerComponent()
            val player    = component.mediaPlayer()

            // Register scaling listener BEFORE play() is called.
            // videoOutput fires on the VLC thread when the vout is ready —
            // the ONLY reliable moment to call setScale.
            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {

                override fun videoOutput(mp: VlcMediaPlayer, newCount: Int) {
                    if (newCount > 0) {
                        mp.video().setScale(0f)          // auto-fit, no 1:1 crop
                        mp.video().setAspectRatio(null)  // keep native AR
                    }
                }

                // playing() is a safety-net in case the canvas was resized
                // between videoOutput and actual frame rendering.
                override fun playing(mp: VlcMediaPlayer) {
                    mp.video().setScale(0f)
                }
            })

            playerComponent = component
            vlcState        = VlcState.Ready
        } catch (e: Exception) {
            vlcState = VlcState.NotFound
        }
    }

    // ── Start playback once the component is ready ────────────────────────────
    LaunchedEffect(vlcState, streamUrl) {
        if (vlcState == VlcState.Ready) {
            val player = playerComponent?.mediaPlayer() ?: return@LaunchedEffect
            // Small delay so SwingPanel has time to attach the component to the
            // real Compose/Skia window before VLC tries to use its HWND.
            delay(150)
            player.media().play(streamUrl)
            isPlaying = true
            resetHideTimer()
        }
    }

    // ── Poll position / duration ──────────────────────────────────────────────
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(500)
            playerComponent?.mediaPlayer()?.let { p ->
                position = p.status().position()
                val len = p.status().length()
                if (len > 0) duration = len
            }
        }
    }

    // ── Stretch toggle ────────────────────────────────────────────────────────
    LaunchedEffect(isStretched) {
        val p = playerComponent?.mediaPlayer() ?: return@LaunchedEffect
        if (isStretched) p.video().setAspectRatio("16:9")
        else             p.video().setAspectRatio(null)
        p.video().setScale(0f)
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            try {
                playerComponent?.mediaPlayer()?.controls()?.stop()
                playerComponent?.release()
            } catch (_: Exception) {}
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        when (vlcState) {

            // ── Loading ───────────────────────────────────────────────────────
            VlcState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Initializing player…", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            // ── VLC not found ─────────────────────────────────────────────────
            VlcState.NotFound -> VlcNotFoundContent(streamUrl, onBack)

            // ── Video panel (SwingPanel wraps EmbeddedMediaPlayerComponent) ───
            VlcState.Ready -> {
                val component = playerComponent
                if (component != null) {
                    // EmbeddedMediaPlayerComponent IS a JPanel — pass it directly.
                    // fillMaxSize() makes it fill the entire Box.
                    SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory  = { component }
                    )
                }
            }
        }

        // ── Always-visible top row: Back ← ··· → Fullscreen ──────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            CircleBtn(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            if (onFullscreenToggle != null) {
                CircleBtn(onClick = onFullscreenToggle) {
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
                    text       = title,
                    color      = Color.White,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(bottom = 2.dp)
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
                    value         = position,
                    onValueChange = { pos ->
                        position = pos
                        playerComponent?.mediaPlayer()?.controls()?.setPosition(pos)
                        resetHideTimer()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor         = Color.White,
                        activeTrackColor   = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Buttons row
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10 s
                    Btn(onClick = {
                        playerComponent?.mediaPlayer()?.controls()?.skipTime(-10_000)
                        resetHideTimer()
                    }) { Icon(Icons.Default.Replay10, "–10 s", tint = Color.White) }

                    // Play / Pause
                    Btn(onClick = {
                        val p = playerComponent?.mediaPlayer() ?: return@Btn
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

                    // Forward 10 s
                    Btn(onClick = {
                        playerComponent?.mediaPlayer()?.controls()?.skipTime(10_000)
                        resetHideTimer()
                    }) { Icon(Icons.Default.Forward10, "+10 s", tint = Color.White) }

                    Spacer(Modifier.weight(1f))

                    // Stretch toggle
                    ToggleChip(
                        label   = "Stretch",
                        active  = isStretched,
                        onClick = { isStretched = !isStretched; resetHideTimer() }
                    )

                    Spacer(Modifier.width(8.dp))

                    // Mute
                    Btn(onClick = {
                        playerComponent?.mediaPlayer()?.audio()?.mute()
                        isMuted = !isMuted
                        resetHideTimer()
                    }) {
                        Icon(
                            if (isMuted) Icons.AutoMirrored.Filled.VolumeMute
                            else         Icons.AutoMirrored.Filled.VolumeUp,
                            if (isMuted) "Unmute" else "Mute",
                            tint = Color.White
                        )
                    }

                    // Stop & back
                    Btn(onClick = {
                        playerComponent?.mediaPlayer()?.controls()?.stop()
                        isPlaying = false
                        onBack()
                    }) { Icon(Icons.Default.Stop, "Stop", tint = Color.White) }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatMillis(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@Composable
private fun CircleBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
private fun Btn(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick) { content() }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f)
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(6.dp),
        color    = bg,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text       = label,
            color      = fg,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun VlcNotFoundContent(streamUrl: String, onBack: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize(),
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
            OutlinedButton(
                onClick = onBack,
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(6.dp))
                Text("Go Back")
            }
            Button(onClick = {
                val paths = listOf(
                    "C:\\Program Files\\VideoLAN\\VLC\\vlc.exe",
                    "C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe"
                )
                val exe = paths.firstOrNull { java.io.File(it).exists() }
                try {
                    if (exe != null) ProcessBuilder(exe, streamUrl).start()
                    else             ProcessBuilder("vlc", streamUrl).start()
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
