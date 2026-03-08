package com.void.desktop.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer as VlcMediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer

// ─────────────────────────────────────────────────────────────────────────────
//  VideoPlayerScreen — Callback-based rendering (no SwingPanel / HWND)
//
//  Uses CallbackMediaPlayerComponent with a RenderCallbackAdapter so VLC
//  delivers every decoded frame as an int[] of BGRA pixels.  We blit that
//  into a Skia Bitmap and expose it as a Compose ImageBitmap, which is
//  then drawn by a regular Compose Image() composable.
//  No native window / HWND embedding → works reliably on Windows CMP.
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

    // Current decoded frame — updated from VLC callback thread, read by Compose
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var frameWidth  by remember { mutableStateOf(1920) }
    var frameHeight by remember { mutableStateOf(1080) }

    // Player reference (callback component — not a JPanel, no HWND)
    var callbackComponent by remember { mutableStateOf<CallbackMediaPlayerComponent?>(null) }

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
    LaunchedEffect(Unit) {
        try {
            val found = NativeDiscovery().discover()
            if (!found) { vlcState = VlcState.NotFound; return@LaunchedEffect }

            // Reusable Skia bitmap (resized when VLC reports a new buffer format)
            val skiaBitmap = Bitmap()

            // ── Buffer format callback — called when VLC knows video dimensions ──
            val bufferFormatCb = object : BufferFormatCallback {
                override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): RV32BufferFormat {
                    frameWidth  = sourceWidth
                    frameHeight = sourceHeight
                    // Allocate Skia bitmap to match
                    skiaBitmap.allocPixels(
                        ImageInfo.makeN32(sourceWidth, sourceHeight, ColorAlphaType.OPAQUE)
                    )
                    return RV32BufferFormat(sourceWidth, sourceHeight)
                }
                override fun allocatedBuffers(buffers: Array<ByteBuffer>) { /* no-op */ }
            }

            // Reusable byte buffer for pixel transfer (avoids allocation per frame)
            var pixelBytes = ByteArray(0)

            // ── Render callback — called for every decoded frame ───────────────
            val renderCb = object : RenderCallbackAdapter() {
                override fun onDisplay(mp: VlcMediaPlayer, rgbBuffer: IntArray) {
                    val w = frameWidth
                    val h = frameHeight
                    if (w <= 0 || h <= 0 || rgbBuffer.size < w * h) return

                    val needed = w * h * 4
                    if (pixelBytes.size != needed) pixelBytes = ByteArray(needed)

                    // Convert IntArray (BGRA ints from RV32) → ByteArray for Skia
                    val bb = java.nio.ByteBuffer.wrap(pixelBytes)
                    bb.order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    val ib = bb.asIntBuffer()
                    ib.put(rgbBuffer, 0, w * h)

                    skiaBitmap.installPixels(skiaBitmap.imageInfo, pixelBytes, w * 4)
                    currentFrame = skiaBitmap.asComposeImageBitmap()
                }
            }

            // CallbackMediaPlayerComponent — pure software path, no HWND
            val component = CallbackMediaPlayerComponent(
                /* mediaPlayerFactory  */ null,
                /* fullScreenStrategy  */ null,
                /* inputEvents         */ null,
                /* lockBuffers         */ true,
                /* renderCallback      */ renderCb,
                /* bufferFormatCallback*/ bufferFormatCb,
                /* videoSurfaceComponent */ null
            )

            val player = component.mediaPlayer()

            // Track play/stop events
            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun playing(mp: VlcMediaPlayer) {
                    isPlaying = true
                }
                override fun paused(mp: VlcMediaPlayer) {
                    isPlaying = false
                }
                override fun stopped(mp: VlcMediaPlayer) {
                    isPlaying = false
                }
                override fun finished(mp: VlcMediaPlayer) {
                    isPlaying = false
                }
                override fun error(mp: VlcMediaPlayer) {
                    isPlaying = false
                }
            })

            callbackComponent = component
            vlcState          = VlcState.Ready
        } catch (e: Exception) {
            vlcState = VlcState.NotFound
        }
    }

    // ── Start playback once ready ─────────────────────────────────────────────
    LaunchedEffect(vlcState, streamUrl) {
        if (vlcState == VlcState.Ready) {
            val player = callbackComponent?.mediaPlayer() ?: return@LaunchedEffect
            delay(100)
            player.media().play(streamUrl)
            isPlaying = true
            resetHideTimer()
        }
    }

    // ── Poll position / duration ──────────────────────────────────────────────
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(500)
            callbackComponent?.mediaPlayer()?.let { p ->
                position = p.status().position()
                val len = p.status().length()
                if (len > 0) duration = len
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            try {
                callbackComponent?.mediaPlayer()?.controls()?.stop()
                callbackComponent?.release()
            } catch (_: Exception) {}
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { resetHideTimer() }
            }
    ) {

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
                val frame = currentFrame
                if (frame == null) {
                    // Waiting for first frame
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    // ── Render decoded frame as Compose Image ─────────────────
                    Image(
                        bitmap = frame,
                        contentDescription = null,
                        contentScale = if (isStretched) ContentScale.FillBounds else ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    val p = callbackComponent?.mediaPlayer() ?: return@detectTapGestures
                                    if (p.status().isPlaying) {
                                        p.controls().pause(); isPlaying = false
                                    } else {
                                        p.controls().play(); isPlaying = true
                                    }
                                    resetHideTimer()
                                }
                            }
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
                        androidx.compose.ui.graphics.Brush.verticalGradient(
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
                        callbackComponent?.mediaPlayer()?.controls()?.setPosition(pos)
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
                        callbackComponent?.mediaPlayer()?.controls()?.skipTime(-10_000)
                        resetHideTimer()
                    }) { Icon(Icons.Default.Replay10, "–10 s", tint = Color.White) }

                    // Play / Pause
                    Btn(onClick = {
                        val p = callbackComponent?.mediaPlayer() ?: return@Btn
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
                        callbackComponent?.mediaPlayer()?.controls()?.skipTime(10_000)
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
                        callbackComponent?.mediaPlayer()?.audio()?.mute()
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
                        callbackComponent?.mediaPlayer()?.controls()?.stop()
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
