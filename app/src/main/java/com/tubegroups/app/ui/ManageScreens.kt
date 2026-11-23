package com.tubegroups.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubegroups.app.model.Channel
import com.tubegroups.app.model.ChannelGroup

@Composable
internal fun ChannelsScreen(
    channels: List<Channel>,
    groups: List<ChannelGroup>,
    onEditGroups: (Channel) -> Unit,
    onDeleteChannel: (Channel) -> Unit,
    onImportCsv: () -> Unit,
    onAddChannel: () -> Unit
) {
    val groupMap = remember(groups) { groups.associateBy { it.id } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onImportCsv,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importar CSV")
            }
            Button(
                onClick = onAddChannel,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo Canal")
            }
        }

        if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Toque em 'Novo Canal' ou importe seu subscriptions.csv.",
                    color = Color.Gray,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    val groupBadges = channel.groupIds.mapNotNull { groupMap[it] }
                        .joinToString("  ") { "${it.emoji} ${it.name}" }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditGroups(channel) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(channel.title, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = groupBadges.ifEmpty { "Sem grupo • toque para organizar" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (groupBadges.isEmpty()) Color.Gray else MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { onDeleteChannel(channel) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GroupsScreen(
    groups: List<ChannelGroup>,
    channels: List<Channel>,
    onDeleteGroup: (ChannelGroup) -> Unit,
    onSelectGroupAndOpenFeed: (String) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum grupo criado. Toque no + para criar.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                val count = channels.count { group.id in it.groupIds }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectGroupAndOpenFeed(group.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(group.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "$count canais",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { onDeleteGroup(group) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir grupo", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
