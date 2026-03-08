package com.void.desktop.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.repository.LibraryRepository
import com.void.desktop.data.repository.Result
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.ui.components.EpisodeCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonDetailScreen(
    seasonId: String,
    seriesName: String,
    prefs: AppPreferences,
    onItemClick: (BaseItemDto) -> Unit,
    onBack: () -> Unit
) {
    val repository = remember { LibraryRepository(prefs) }
    val coroutineScope = rememberCoroutineScope()

    var episodes by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadEpisodes() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.getItem(seasonId)) {
                is Result.Success -> {
                    val season = result.data
                    when (val episodesResult = repository.getEpisodes(season.seriesId ?: "", seasonId)) {
                        is Result.Success -> episodes = episodesResult.data
                        is Result.Error -> errorMessage = episodesResult.message
                    }
                }
                is Result.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(seasonId) { loadEpisodes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(seriesName) },
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
                        Button(onClick = { loadEpisodes() }) { Text("Retry") }
                    }
                }
            }
            episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No episodes found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(episodes.size) { index ->
                            EpisodeCard(
                                episode = episodes[index],
                                prefs = prefs,
                                onClick = { onItemClick(episodes[index]) }
                            )
                        }
                    }
                }
            }
        }
    }
}
