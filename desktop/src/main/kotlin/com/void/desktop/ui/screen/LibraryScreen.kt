package com.void.desktop.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.repository.LibraryRepository
import com.void.desktop.data.repository.Result
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.ui.components.MediaCard
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    library: BaseItemDto,
    prefs: AppPreferences,
    onItemClick: (BaseItemDto) -> Unit,
    onBack: () -> Unit
) {
    val repository = remember { LibraryRepository(prefs) }
    val coroutineScope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }
    var startIndex by remember { mutableStateOf(0) }

    fun loadInitial() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            startIndex = 0
            when (val result = repository.getLibraryItems(library.id, startIndex = 0, limit = PAGE_SIZE)) {
                is Result.Success -> {
                    items = result.data
                    startIndex = result.data.size
                    hasMore = result.data.size >= PAGE_SIZE
                }
                is Result.Error -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        coroutineScope.launch {
            isLoadingMore = true
            when (val result = repository.getLibraryItems(library.id, startIndex = startIndex, limit = PAGE_SIZE)) {
                is Result.Success -> {
                    items = items + result.data
                    startIndex += result.data.size
                    hasMore = result.data.size >= PAGE_SIZE
                }
                is Result.Error -> errorMessage = result.message
            }
            isLoadingMore = false
        }
    }

    LaunchedEffect(library.id) { loadInitial() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(library.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoading) {
                        Text(
                            text = "${items.size} items",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = { loadInitial() }) {
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
                        Button(onClick = { loadInitial() }) { Text("Retry") }
                    }
                }
            }

            items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No items found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    items(items) { item ->
                        MediaCard(
                            item = item,
                            imageUrl = getPrimaryImageUrl(item, prefs),
                            onClick = { onItemClick(item) }
                        )
                    }

                    // Footer: Load More button or end indicator
                    if (hasMore || isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                } else {
                                    OutlinedButton(
                                        onClick = { loadMore() },
                                        modifier = Modifier.width(200.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Load More")
                                    }
                                }
                            }
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "All ${items.size} items loaded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
