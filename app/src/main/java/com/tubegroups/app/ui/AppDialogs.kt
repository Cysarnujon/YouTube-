package com.tubegroups.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubegroups.app.model.Channel
import com.tubegroups.app.model.ChannelGroup

@Composable
internal fun AddChannelDialog(
    groups: List<ChannelGroup>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar canal") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("@handle, URL ou ID (UC...)") },
                    placeholder = { Text("Ex: @manualdomundo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (groups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Adicionar aos grupos:", style = MaterialTheme.typography.labelLarge)
                    groups.forEach { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (group.id in selectedIds) selectedIds.remove(group.id)
                                    else selectedIds.add(group.id)
                                }
                        ) {
                            Checkbox(
                                checked = group.id in selectedIds,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(group.id) else selectedIds.remove(group.id)
                                }
                            )
                            Text("${group.emoji} ${group.name}")
                        }
                    }
                }
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = input.isNotBlank() && !isLoading,
                onClick = { onConfirm(input, selectedIds.toList()) }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
internal fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val emojis = listOf("📁", "💻", "🎮", "🎵", "🎬", "⚽", "📰", "💡", "🚀", "📚")
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo grupo") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do grupo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { emoji ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (emoji == selectedEmoji)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedEmoji = emoji }
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, selectedEmoji) }
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
internal fun AssignGroupsDialog(
    channel: Channel,
    groups: List<ChannelGroup>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val selectedIds = remember(channel) {
        mutableStateListOf<String>().apply { addAll(channel.groupIds) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grupos de ${channel.title}") },
        text = {
            if (groups.isEmpty()) {
                Text("Crie um grupo na aba 'Grupos' primeiro.")
            } else {
                Column {
                    groups.forEach { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (group.id in selectedIds) selectedIds.remove(group.id)
                                    else selectedIds.add(group.id)
                                }
                        ) {
                            Checkbox(
                                checked = group.id in selectedIds,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(group.id) else selectedIds.remove(group.id)
                                }
                            )
                            Text("${group.emoji} ${group.name}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedIds.toList()) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
