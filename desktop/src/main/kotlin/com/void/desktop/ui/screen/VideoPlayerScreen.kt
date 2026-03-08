package com.void.desktop.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
    isFullscreen: Boolean = false,
    customVlcPath: String = ""
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
            println("=== Initializing VLC ===")

            // First, try to find VLC and set the plugin path
            val vlcInfo = findVlcPath(customVlcPath)

            if (vlcInfo == null) {
                println("Could not find VLC installation")
                vlcState = VlcState.NotFound
                return@LaunchedEffect
            }

            println("Found VLC at: ${vlcInfo.vlcDir}")
            println("Plugins at: ${vlcInfo.pluginDir}")

            // Try to load native libraries explicitly
            if (!loadVlcNativeLibraries(vlcInfo)) {
                println("Failed to load VLC native libraries")
                vlcState = VlcState.NotFound
                return@LaunchedEffect
            }

            // Create MediaPlayerFactory with explicit plugin path
            println("Creating MediaPlayerFactory...")
            val args = mutableListOf("--no-video-title-show", "--quiet", "--no-xlib")
            args.add("--plugin-path=${vlcInfo.pluginDir}")
            args.addAll(currentAspectRatio.vlcArgs)
            println("VLC args: ${args.joinToString(" ")}")

            try {
                val factory = MediaPlayerFactory(*args.toTypedArray())
                val player = factory.mediaPlayers().newEmbeddedMediaPlayer()

                mediaPlayerFactory = factory
                mediaPlayer = player
                vlcState = VlcState.Ready
                println("VLC initialized successfully")
            } catch (e: Exception) {
                println("MediaPlayerFactory creation failed: ${e.message}")
                e.printStackTrace()
                vlcState = VlcState.NotFound
            }
        } catch (e: Exception) {
            println("VLC initialization error: ${e.message}")
            e.printStackTrace()
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
    var diagnosticInfo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val info = buildString {
            val vlcPaths = mutableListOf<String>()

            // Standard paths
            vlcPaths.add("C:\\Program Files\\VideoLAN\\VLC")
            vlcPaths.add("C:\\Program Files (x86)\\VideoLAN\\VLC")
            System.getenv("LOCALAPPDATA")?.let {
                vlcPaths.add("$it\\Programs\\VideoLAN\\VLC")
            }
            System.getenv("ProgramFiles")?.let {
                vlcPaths.add("$it\\VideoLAN\\VLC")
            }
            System.getenv("ProgramFiles(x86)")?.let {
                vlcPaths.add("$it\\VideoLAN\\VLC")
            }

            appendLine("VLC Diagnostic Information:")
            appendLine("")

            // Check if VLC is in PATH
            try {
                val process = ProcessBuilder("where", "vlc").start()
                val output = process.inputStream.bufferedReader().readText()
                if (output.isNotBlank()) {
                    appendLine("✓ VLC found in PATH: ${output.lines().first()}")
                    appendLine("")
                }
            } catch (e: Exception) {
                appendLine("✗ VLC not found in PATH")
                appendLine("")
            }

            vlcPaths.distinct().forEach { path ->
                val vlcDir = java.io.File(path)
                val pluginsDir = java.io.File(path, "plugins")
                val libvlccore = java.io.File(path, "libvlccore.dll")
                val libvlc = java.io.File(path, "libvlc.dll")
                val vlcExe = java.io.File(path, "vlc.exe")

                appendLine("Path: $path")
                appendLine("  Directory: ${if (vlcDir.exists()) "✓" else "✗"}")
                appendLine("  vlc.exe: ${if (vlcExe.exists()) "✓" else "✗"}")
                appendLine("  plugins/: ${if (pluginsDir.exists()) "✓" else "✗"}")
                appendLine("  libvlc.dll: ${if (libvlc.exists()) "✓" else "✗"}")
                appendLine("  libvlccore.dll: ${if (libvlccore.exists()) "✓" else "✗"}")
                appendLine("")
            }

            appendLine("System Properties:")
            appendLine("  java.library.path: ${System.getProperty("java.library.path")}")
            appendLine("  vlc.plugin.path: ${System.getProperty("vlc.plugin.path", "Not set")}")
            appendLine("  user.home: ${System.getProperty("user.home")}")
        }
        diagnosticInfo = info
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "VLC initialization failed",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "VLC is installed but its native libraries could not be loaded",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Try setting the VLC path manually in Settings > Playback",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(24.dp))

        // Diagnostic section
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Diagnostic Information",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    diagnosticInfo,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(24.dp))

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
                val vlcPaths = mutableListOf<String>()
                vlcPaths.add("C:\\Program Files\\VideoLAN\\VLC\\vlc.exe")
                vlcPaths.add("C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe")
                System.getenv("LOCALAPPDATA")?.let {
                    vlcPaths.add("$it\\Programs\\VideoLAN\\VLC\\vlc.exe")
                }
                System.getenv("ProgramFiles")?.let {
                    vlcPaths.add("$it\\VideoLAN\\VLC\\vlc.exe")
                }
                System.getenv("ProgramFiles(x86)")?.let {
                    vlcPaths.add("$it\\VideoLAN\\VLC\\vlc.exe")
                }

                val vlcExe = vlcPaths.firstOrNull { java.io.File(it).exists() }
                try {
                    if (vlcExe != null) {
                        ProcessBuilder(vlcExe, streamUrl).start()
                    } else {
                        ProcessBuilder("vlc", streamUrl).start()
                    }
                } catch (_: Exception) {
                    javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Could not launch VLC. Please open VLC manually and play:\n\n${streamUrl}",
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    )
                }
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

// Manual VLC discovery - check common installation paths
fun discoverVlcManually(): Boolean {
    println("=== Starting manual VLC discovery ===")

    // Try to find VLC by checking PATH first
    try {
        val process = ProcessBuilder("where", "vlc").start()
        val output = process.inputStream.bufferedReader().readText()
        if (output.isNotBlank()) {
            val vlcExePath = output.lines().first()
            val vlcDir = java.io.File(vlcExePath).parent
            println("Found VLC in PATH: $vlcDir")
            if (validateAndSetVlcPath(vlcDir)) return true
        }
    } catch (e: Exception) {
        println("VLC not found in PATH: ${e.message}")
    }

    // Check standard installation paths
    val vlcPaths = mutableListOf<String>()

    // Standard paths
    vlcPaths.add("C:\\Program Files\\VideoLAN\\VLC")
    vlcPaths.add("C:\\Program Files (x86)\\VideoLAN\\VLC")
    System.getenv("LOCALAPPDATA")?.let {
        vlcPaths.add("$it\\Programs\\VideoLAN\\VLC")
    }

    // Environment-based paths
    System.getenv("ProgramFiles")?.let {
        vlcPaths.add("$it\\VideoLAN\\VLC")
    }
    System.getenv("ProgramFiles(x86)")?.let {
        vlcPaths.add("$it\\VideoLAN\\VLC")
    }

    for (path in vlcPaths.distinct()) {
        if (validateAndSetVlcPath(path)) return true
    }

    println("=== VLC not found in any location ===")
    return false
}

// Data class to hold VLC path information
data class VlcInfo(val vlcDir: String, val pluginDir: String)

// Find VLC installation - returns null if not found
fun findVlcPath(customPath: String = ""): VlcInfo? {
    println("=== Finding VLC installation ===")

    val vlcPaths = mutableListOf<String>()

    // Custom path first
    if (customPath.isNotBlank()) {
        vlcPaths.add(customPath.trim())
    }

    // Standard paths
    vlcPaths.add("C:\\Program Files\\VideoLAN\\VLC")
    vlcPaths.add("C:\\Program Files (x86)\\VideoLAN\\VLC")
    System.getenv("LOCALAPPDATA")?.let {
        vlcPaths.add("$it\\Programs\\VideoLAN\\VLC")
    }
    System.getenv("ProgramFiles")?.let {
        vlcPaths.add("$it\\VideoLAN\\VLC")
    }
    System.getenv("ProgramFiles(x86)")?.let {
        vlcPaths.add("$it\\VideoLAN\\VLC")
    }

    // Check PATH
    try {
        val process = ProcessBuilder("where", "vlc").start()
        val output = process.inputStream.bufferedReader().readText()
        if (output.isNotBlank()) {
            val vlcExePath = output.lines().first()
            val vlcDir = java.io.File(vlcExePath).parent
            println("Found VLC in PATH: $vlcDir")
            if (customPath.isBlank()) vlcPaths.add(0, vlcDir)
        }
    } catch (e: Exception) {
        println("VLC not in PATH")
    }

    for (path in vlcPaths.distinct()) {
        val vlcDir = java.io.File(path)
        val pluginDir = java.io.File(path, "plugins")
        val libvlccore = java.io.File(path, "libvlccore.dll")
        val libvlc = java.io.File(path, "libvlc.dll")

        println("Checking: $path")
        println("  Directory: ${vlcDir.exists()}")
        println("  plugins/: ${pluginDir.exists()}")
        println("  libvlc.dll: ${libvlc.exists()}")
        println("  libvlccore.dll: ${libvlccore.exists()}")

        if (vlcDir.exists() && pluginDir.exists() && libvlc.exists() && libvlccore.exists()) {
            println("✓ Found complete VLC installation at: $path")
            return VlcInfo(path, pluginDir.absolutePath)
        }
    }

    println("✗ Complete VLC installation not found")
    return null
}

// Load VLC native libraries explicitly
fun loadVlcNativeLibraries(vlcInfo: VlcInfo): Boolean {
    println("=== Setting up VLC native libraries ===")

    try {
        // Set system properties BEFORE any VLCJ initialization
        System.setProperty("java.library.path", vlcInfo.vlcDir)
        System.setProperty("vlc.plugin.path", vlcInfo.pluginDir)

        println("System properties set:")
        println("  java.library.path: ${vlcInfo.vlcDir}")
        println("  vlc.plugin.path: ${vlcInfo.pluginDir}")

        // Try to copy VLC DLLs to temp directory to help with loading
        println("\nAttempting to copy VLC DLLs to temp directory...")
        try {
            val tempDir = java.io.File(System.getProperty("java.io.tmpdir"))
            val libvlccore = java.io.File(vlcInfo.vlcDir, "libvlccore.dll")
            val libvlc = java.io.File(vlcInfo.vlcDir, "libvlc.dll")

            val tempLibvlccore = java.io.File(tempDir, "libvlccore.dll")
            val tempLibvlc = java.io.File(tempDir, "libvlc.dll")

            if (libvlccore.exists()) {
                libvlccore.copyTo(tempLibvlccore, overwrite = true)
                println("  Copied libvlccore.dll to: ${tempLibvlccore.absolutePath}")
            }
            if (libvlc.exists()) {
                libvlc.copyTo(tempLibvlc, overwrite = true)
                println("  Copied libvlc.dll to: ${tempLibvlc.absolutePath}")
            }

            // Add temp dir to library path
            System.setProperty("java.library.path", tempDir.absolutePath)
            println("  Updated java.library.path to: ${tempDir.absolutePath}")

        } catch (e: Exception) {
            println("  Warning: Could not copy DLLs to temp: ${e.message}")
        }

        // Try NativeDiscovery to verify VLC can be found
        println("\nTrying NativeDiscovery...")
        val discovered = try {
            NativeDiscovery().discover()
        } catch (e: Exception) {
            println("  Discovery error: ${e.message}")
            false
        }

        println("  Discovery result: $discovered")

        if (discovered) {
            println("✓ VLC libraries found and can be loaded")
        } else {
            println("⚠ NativeDiscovery failed, but will try anyway...")
        }

        return true

    } catch (e: Exception) {
        println("✗ Failed to setup libraries: ${e.message}")
        e.printStackTrace()
        return false
    }
}

// Find VLC plugin path - returns null if not found (deprecated, use findVlcPath instead)
@Deprecated("Use findVlcPath instead")
fun findVlcPluginPath(customPath: String = ""): String? {
    println("=== Finding VLC plugin path ===")
    println("Custom path: \"$customPath\"")

    // List of possible VLC locations
    val vlcPaths = mutableListOf<String>()

    // If custom path is provided, check it first
    if (customPath.isNotBlank()) {
        println("Checking custom path first...")
        vlcPaths.add(0, customPath.trim())
    }

    // Standard paths
    vlcPaths.add("C:\\Program Files\\VideoLAN\\VLC")
    vlcPaths.add("C:\\Program Files (x86)\\VideoLAN\\VLC")
    System.getenv("LOCALAPPDATA")?.let {
        vlcPaths.add("$it\\Programs\\VideoLAN\\VLC")
    }
    System.getenv("ProgramFiles")?.let {
        vlcPaths.add("$it\\VideoLAN\\VLC")
    }
    System.getenv("ProgramFiles(x86)")?.let {
        vlcPaths.add("$it\\VideoLAN\\VLC")
    }

    // Try to find VLC in PATH
    try {
        val process = ProcessBuilder("where", "vlc").start()
        val output = process.inputStream.bufferedReader().readText()
        if (output.isNotBlank()) {
            val vlcExePath = output.lines().first()
            val vlcDir = java.io.File(vlcExePath).parent
            println("Found VLC in PATH: $vlcDir")
            if (customPath.isBlank()) {
                vlcPaths.add(0, vlcDir) // Add at the beginning if no custom path
            } else {
                vlcPaths.add(vlcDir)
            }
        }
    } catch (e: Exception) {
        println("VLC not found in PATH")
    }

    for (path in vlcPaths.distinct()) {
        val pluginsDir = java.io.File(path, "plugins")
        val libvlccore = java.io.File(path, "libvlccore.dll")
        val libvlc = java.io.File(path, "libvlc.dll")

        println("Checking: $path")
        println("  plugins/: ${pluginsDir.exists()}")
        println("  libvlc.dll: ${libvlc.exists()}")
        println("  libvlccore.dll: ${libvlccore.exists()}")

        if (pluginsDir.exists() && (libvlccore.exists() || libvlc.exists())) {
            println("✓ Found VLC at: $path")

            // Set system properties
            System.setProperty("vlc.plugin.path", pluginsDir.absolutePath)
            System.setProperty("java.library.path", path)

            return pluginsDir.absolutePath
        }
    }

    println("✗ VLC not found")
    return null
}

private fun validateAndSetVlcPath(path: String): Boolean {
    val vlcDir = java.io.File(path)
    val pluginsDir = java.io.File(path, "plugins")
    val libvlccore = java.io.File(path, "libvlccore.dll")
    val libvlc = java.io.File(path, "libvlc.dll")

    println("Checking VLC path: $path")
    println("  Directory exists: ${vlcDir.exists()}")
    println("  libvlccore.dll exists: ${libvlccore.exists()}")
    println("  libvlc.dll exists: ${libvlc.exists()}")
    println("  plugins/ exists: ${pluginsDir.exists()}")

    if (vlcDir.exists() && (libvlccore.exists() || libvlc.exists())) {
        println("✓ Found VLC at: $path")

        // Set plugin path
        if (pluginsDir.exists()) {
            System.setProperty("vlc.plugin.path", pluginsDir.absolutePath)
            println("  Set VLC plugin path: ${pluginsDir.absolutePath}")
        }

        // Add to java.library.path
        try {
            System.setProperty("java.library.path", path)
            println("  Set java.library.path: $path")
        } catch (e: Exception) {
            println("  Warning: Could not set java.library.path: ${e.message}")
        }

        return true
    }

    return false
}
