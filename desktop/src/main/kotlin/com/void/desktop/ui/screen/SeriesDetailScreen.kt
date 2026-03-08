package com.void.desktop.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.void.desktop.data.api.ApiClient
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.repository.LibraryRepository
import com.void.desktop.data.repository.Result
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.ui.components.EpisodeCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: String,
    prefs: AppPreferences,
    onSeasonClick: (seasonId: String, seasonName: String) -> Unit,
    onEpisodeClick: (episodeId: String) -> Unit,
    onBack: () -> Unit
) {
    val repository = remember { LibraryRepository(prefs) }
    val coroutineScope = rememberCoroutineScope()

    var series by remember { mutableStateOf<BaseItemDto?>(null) }
    var seasons by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var selectedSeasonEpisodes by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<BaseItemDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadSeries() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                when (val result = repository.getItem(seriesId)) {
                    is Result.Success -> {
                        series = result.data
                        when (val seasonsResult = repository.getSeasons(seriesId)) {
                            is Result.Success -> {
                                seasons = seasonsResult.data
                                if (seasons.isNotEmpty()) {
                                    selectedSeason = seasons.first()
                                    when (val episodesResult = repository.getEpisodes(seriesId, selectedSeason?.id)) {
                                        is Result.Success -> selectedSeasonEpisodes = episodesResult.data
                                        is Result.Error -> errorMessage = episodesResult.message
                                    }
                                }
                            }
                            is Result.Error -> errorMessage = seasonsResult.message
                        }
                    }
                    is Result.Error -> errorMessage = result.message
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun loadSeasonEpisodes(season: BaseItemDto) {
        coroutineScope.launch {
            when (val result = repository.getEpisodes(seriesId, season.id)) {
                is Result.Success -> {
                    selectedSeason = season
                    selectedSeasonEpisodes = result.data
                }
                is Result.Error -> errorMessage = result.message
            }
        }
    }

    LaunchedEffect(seriesId) { loadSeries() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(series?.displayName ?: "Series") },
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
                        Button(onClick = { loadSeries() }) { Text("Retry") }
                    }
                }
            }
            series != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    // Series backdrop and info
                    item {
                        val backdropUrl = if (series!!.backdropImageTags.isNotEmpty()) {
                            ApiClient.buildBackdropUrl(
                                serverUrl = prefs.serverUrl,
                                itemId = series!!.id,
                                tag = series!!.backdropImageTags.first(),
                                accessToken = prefs.accessToken
                            )
                        } else null

                        if (backdropUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                AsyncImage(
                                    model = backdropUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }

                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = series!!.displayName,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            val overview = series!!.overview
                            if (!overview.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Seasons selector
                    if (seasons.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "Seasons",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(seasons) { season ->
                                        FilterChip(
                                            selected = selectedSeason?.id == season.id,
                                            onClick = { loadSeasonEpisodes(season) },
                                            label = { Text(season.displayName) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Episodes
                    if (selectedSeasonEpisodes.isNotEmpty()) {
                        item {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(selectedSeasonEpisodes) { episode ->
                            EpisodeCard(
                                episode = episode,
                                prefs = prefs,
                                onClick = { onEpisodeClick(episode.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

