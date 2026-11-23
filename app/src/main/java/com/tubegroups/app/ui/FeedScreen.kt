package com.tubegroups.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tubegroups.app.model.VideoItem

@Composable
internal fun FeedScreen(
    state: MainUiState,
    onSelectGroup: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleHideShorts: () -> Unit,
    onToggleHideWatched: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onGoToChannels: () -> Unit
) {
    val selectedGroupId = state.selectedGroupId
    val allowedChannelIds = remember(state.channels, selectedGroupId) {
        when (selectedGroupId) {
            null -> state.channels.map { it.id }.toSet()
            "ungrouped" -> state.channels.filter { it.groupIds.isEmpty() }.map { it.id }.toSet()
            else -> state.channels.filter { selectedGroupId in it.groupIds }.map { it.id }.toSet()
        }
    }

    val filteredVideos = remember(
        state.videos,
        allowedChannelIds,
        state.searchQuery,
        state.hideShorts,
        state.hideWatched,
        state.watchedIds
    ) {
        val query = state.searchQuery.trim().lowercase()
        state.videos.filter { video ->
            video.channelId in allowedChannelIds &&
                (!state.hideShorts || !video.isShort) &&
                (!state.hideWatched || video.id !in state.watchedIds) &&
                (query.isEmpty() ||
                    video.title.lowercase().contains(query) ||
                    video.channelTitle.lowercase().contains(query))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isLoadingFeed) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = { Text("Buscar vídeo ou canal...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedGroupId == null,
                    onClick = { onSelectGroup(null) },
                    label = { Text("Todos") }
                )
            }
            items(state.groups, key = { it.id }) { group ->
                FilterChip(
                    selected = selectedGroupId == group.id,
                    onClick = { onSelectGroup(group.id) },
                    label = { Text("${group.emoji} ${group.name}") }
                )
            }
            item {
                FilterChip(
                    selected = selectedGroupId == "ungrouped",
                    onClick = { onSelectGroup("ungrouped") },
                    label = { Text("Sem grupo") }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.hideShorts,
                onClick = onToggleHideShorts,
                label = { Text("Sem Shorts", fontSize = 12.sp) }
            )
            FilterChip(
                selected = state.hideWatched,
                onClick = onToggleHideWatched,
                label = { Text("Ocultar assistidos", fontSize = 12.sp) }
            )
        }

        if (state.channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Nenhum canal adicionado ainda",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Adicione canais pelo @handle ou importe o subscriptions.csv do Google Takeout.",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onGoToChannels) {
                        Text("Ir para Canais")
                    }
                }
            }
        } else if (filteredVideos.isEmpty() && !state.isLoadingFeed) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum vídeo encontrado para esse filtro.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    val watched = video.id in state.watchedIds
                    VideoCard(
                        video = video,
                        isWatched = watched,
                        onClick = { onOpenVideo(video) },
                        onToggleWatched = { onToggleWatched(video) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: VideoItem,
    isWatched: Boolean,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
                if (video.isShort) {
                    Surface(
                        color = Color(0xCCFF0000),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SHORTS",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isWatched) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${video.channelTitle} • ${video.publishedLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onToggleWatched) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Marcar como assistido",
                        tint = if (isWatched) MaterialTheme.colorScheme.secondary else Color.DarkGray
                    )
                }
            }
        }
    }
}
