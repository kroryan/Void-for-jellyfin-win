package com.void.desktop.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.void.desktop.data.api.ApiClient
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.repository.LibraryRepository
import com.void.desktop.data.repository.Result
import com.void.desktop.data.storage.AppPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    itemId: String,
    prefs: AppPreferences,
    onPlay: (streamUrl: String, title: String) -> Unit,
    onBack: () -> Unit
) {
    val repository = remember { LibraryRepository(prefs) }
    val coroutineScope = rememberCoroutineScope()

    var item by remember { mutableStateOf<BaseItemDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    var isPlayed by remember { mutableStateOf(false) }
    var isPlayLoading by remember { mutableStateOf(false) }

    fun loadItem() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.getItem(itemId)) {
                is Result.Success -> {
                    item = result.data
                    isFavorite = result.data.userData?.isFavorite ?: false
                    isPlayed = result.data.userData?.played ?: false
                }
                is Result.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun play() {
        coroutineScope.launch {
            isPlayLoading = true
            when (val result = repository.getStreamUrl(itemId)) {
                is Result.Success -> {
                    isPlayLoading = false
                    onPlay(result.data, item?.displayName ?: "")
                }
                is Result.Error -> {
                    isPlayLoading = false
                    errorMessage = result.message
                }
            }
        }
    }

    fun toggleFavorite() {
        coroutineScope.launch {
            repository.markFavorite(itemId, !isFavorite)
            isFavorite = !isFavorite
        }
    }

    fun togglePlayed() {
        coroutineScope.launch {
            if (isPlayed) {
                repository.markUnplayed(itemId)
            } else {
                repository.markPlayed(itemId)
            }
            isPlayed = !isPlayed
        }
    }

    LaunchedEffect(itemId) { loadItem() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { loadItem() }) { Text("Retry") }
                    }
                }
            }
            item != null -> {
                val currentItem = item!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        val backdropUrl = if (currentItem.backdropImageTags.isNotEmpty()) {
                            ApiClient.buildBackdropUrl(
                                serverUrl = prefs.serverUrl,
                                itemId = currentItem.id,
                                tag = currentItem.backdropImageTags.first(),
                                accessToken = prefs.accessToken
                            )
                        } else null

                        if (backdropUrl != null) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 100f
                                    )
                                )
                        )
                    }

                    // Content
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        // Title
                        Text(
                            text = currentItem.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Meta info
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentItem.productionYear != null) {
                                Text(
                                    text = currentItem.productionYear.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (currentItem.runtimeMinutes != null) {
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${currentItem.runtimeMinutes}m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (currentItem.communityRating != null) {
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "%.1f".format(currentItem.communityRating),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Genres
                        if (currentItem.genres.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentItem.genres.take(4).forEach { genre ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(genre, style = MaterialTheme.typography.labelSmall)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { play() },
                                enabled = !isPlayLoading
                            ) {
                                if (isPlayLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    val hasProgress = (currentItem.userData?.playbackPositionTicks ?: 0L) > 0L
                                    Text(if (hasProgress) "Resume" else "Play")
                                }
                            }

                            IconButton(onClick = { toggleFavorite() }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(onClick = { togglePlayed() }) {
                                Icon(
                                    imageVector = if (isPlayed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isPlayed) "Mark as unplayed" else "Mark as played",
                                    tint = if (isPlayed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Tagline
                        if (currentItem.taglines.isNotEmpty()) {
                            Text(
                                text = currentItem.taglines.first(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Light
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        // Overview
                        if (!currentItem.overview.isNullOrBlank()) {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentItem.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        // Cast
                        val cast = currentItem.people.filter { it.type == "Actor" }.take(8)
                        if (cast.isNotEmpty()) {
                            Text(
                                text = "Cast",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = cast.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        // Media info
                        val videoStream = currentItem.mediaSources.firstOrNull()
                            ?.mediaStreams?.firstOrNull { it.type == "Video" }
                        if (videoStream != null) {
                            Text(
                                text = "Video",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(4.dp))
                            val info = buildString {
                                videoStream.codec?.let { append(it.uppercase()) }
                                if (videoStream.width != null && videoStream.height != null) {
                                    append(" ${videoStream.width}x${videoStream.height}")
                                }
                                videoStream.bitRate?.let { append(" ${it / 1000}kbps") }
                            }
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
