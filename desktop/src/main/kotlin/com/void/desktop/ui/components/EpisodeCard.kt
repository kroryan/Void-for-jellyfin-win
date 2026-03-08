package com.void.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.void.desktop.data.api.ApiClient
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.storage.AppPreferences

@Composable
fun EpisodeCard(
    episode: BaseItemDto,
    prefs: AppPreferences,
    onClick: () -> Unit
) {
    val imageUrl = episode.imageTags?.primary?.let {
        ApiClient.buildImageUrl(
            serverUrl = prefs.serverUrl,
            itemId = episode.id,
            imageType = "Primary",
            tag = it,
            accessToken = prefs.accessToken,
            maxWidth = 300
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episode.displayName,
                    modifier = Modifier
                        .width(120.dp)
                        .height(68.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(
                    text = "S${episode.parentIndexNumber ?: 0} · E${episode.indexNumber ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = episode.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val overview = episode.overview
                if (overview != null) {
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
