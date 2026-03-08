package com.void.desktop.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    val gridState = rememberLazyGridState()

    var items by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 50
    var hasMoreItems by remember { mutableStateOf(true) }

    fun loadItems(reset: Boolean = false) {
        coroutineScope.launch {
            if (reset) {
                currentPage = 0
                items = emptyList()
                hasMoreItems = true
                isLoading = true
            } else {
                isLoadingMore = true
            }
            errorMessage = null

            val startIndex = currentPage * pageSize

            when (val result = repository.getLibraryItems(
                libraryId = library.id,
                startIndex = startIndex,
                limit = pageSize
            )) {
                is Result.Success -> {
                    if (reset) {
                        items = result.data
                    } else {
                        items = items + result.data
                    }
                    hasMoreItems = result.data.size >= pageSize
                    currentPage++
                }
                is Result.Error -> errorMessage = result.message
            }

            isLoading = false
            isLoadingMore = false
        }
    }

    // Load more when reaching the end
    LaunchedEffect(gridState.firstVisibleItemIndex) {
        if (!isLoading && !isLoadingMore && hasMoreItems &&
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == items.size - 1) {
            loadItems(reset = false)
        }
    }

    LaunchedEffect(library.id) { loadItems(reset = true) }

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
                    IconButton(onClick = { loadItems(reset = true) }) {
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
            isLoading && items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null && items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { loadItems(reset = true) }) { Text("Retry") }
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
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    items(items) { item ->
                        val imageUrl = getPrimaryImageUrl(item, prefs)
                        MediaCard(
                            item = item,
                            imageUrl = imageUrl,
                            onClick = { onItemClick(item) }
                        )
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
