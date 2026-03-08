package com.void.desktop.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.void.desktop.data.api.ApiClient
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.repository.LibraryRepository
import com.void.desktop.data.repository.Result
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.ui.components.MediaCard
import com.void.desktop.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: AppPreferences,
    onItemClick: (BaseItemDto) -> Unit,
    onLibraryClick: (BaseItemDto) -> Unit
) {
    val repository = remember { LibraryRepository(prefs) }
    val coroutineScope = rememberCoroutineScope()

    var libraries by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var resumeItems by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var latestItems by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                when (val libs = repository.getLibraries()) {
                    is Result.Success -> libraries = libs.data
                    is Result.Error   -> errorMessage = libs.message
                }
                when (val resume = repository.getResumeItems()) {
                    is Result.Success -> resumeItems = resume.data
                    is Result.Error   -> { /* optional */ }
                }
                when (val latest = repository.getLatestItems()) {
                    is Result.Success -> latestItems = latest.data
                    is Result.Error   -> { /* optional */ }
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Void for Jellyfin",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                        Button(onClick = { loadData() }) { Text("Retry") }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Greeting
                    Text(
                        text = "Welcome back, ${prefs.userName}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    // Libraries
                    if (libraries.isNotEmpty()) {
                        SectionHeader("Your Libraries")
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            libraries.forEach { library ->
                                val imageUrl = library.imageTags?.primary?.let {
                                    ApiClient.buildImageUrl(
                                        serverUrl = prefs.serverUrl,
                                        itemId = library.id,
                                        imageType = "Primary",
                                        tag = it,
                                        accessToken = prefs.accessToken,
                                        maxWidth = 300
                                    )
                                }
                                MediaCard(
                                    item = library,
                                    imageUrl = imageUrl,
                                    onClick = { onLibraryClick(library) },
                                    cardWidth = 140.dp,
                                    cardHeight = 90.dp
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Continue Watching
                    if (resumeItems.isNotEmpty()) {
                        SectionHeader("Continue Watching")
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            resumeItems.forEach { item ->
                                MediaCard(
                                    item = item,
                                    imageUrl = getPrimaryImageUrl(item, prefs),
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Recently Added
                    if (latestItems.isNotEmpty()) {
                        SectionHeader("Recently Added")
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            latestItems.forEach { item ->
                                MediaCard(
                                    item = item,
                                    imageUrl = getPrimaryImageUrl(item, prefs),
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // Empty state
                    if (libraries.isEmpty() && resumeItems.isEmpty() && latestItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No content found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getPrimaryImageUrl(item: BaseItemDto, prefs: AppPreferences): String? {
    val tag = item.imageTags?.primary
        ?: item.seriesPrimaryImageTag
        ?: return null
    val itemId = if (item.imageTags?.primary != null) item.id else (item.seriesId ?: item.id)
    return ApiClient.buildImageUrl(
        serverUrl = prefs.serverUrl,
        itemId = itemId,
        imageType = "Primary",
        tag = tag,
        accessToken = prefs.accessToken
    )
}
