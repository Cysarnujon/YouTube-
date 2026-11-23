package com.tubegroups.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tubegroups.app.model.Channel
import com.tubegroups.app.model.ChannelGroup
import com.tubegroups.app.model.VideoItem

private val TubeDarkScheme = darkColorScheme(
    primary = Color(0xFFFF4444),
    onPrimary = Color.White,
    secondary = Color(0xFF3EA6FF),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF181818),
    surfaceVariant = Color(0xFF272727),
    onBackground = Color(0xFFF1F1F1),
    onSurface = Color(0xFFF1F1F1)
)

@Composable
fun TubeGroupsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TubeDarkScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TubeGroupsApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableIntStateOf(0) }
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<Channel?>(null) }
    var activeVideo by remember { mutableStateOf<VideoItem?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importTakeoutCsv(uri)
        }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val currentPlaying = activeVideo
    if (currentPlaying != null) {
        val upNext = remember(state.videos, currentPlaying.id) {
            state.videos.filter { it.id != currentPlaying.id }.take(20)
        }
        VideoPlayerScreen(
            video = currentPlaying,
            upNextVideos = upNext,
            onSelectVideo = { nextVideo ->
                viewModel.markVideoWatched(nextVideo.id)
                activeVideo = nextVideo
            },
            onClose = { activeVideo = null }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TubeGroups",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (currentTab == 0) {
                        IconButton(onClick = { viewModel.refreshFeed() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar feed")
                        }
                    } else if (currentTab == 1) {
                        IconButton(onClick = {
                            csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                        }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Importar CSV")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Subscriptions, contentDescription = null) },
                    label = { Text("Canais (${state.channels.size})") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Grupos (${state.groups.size})") }
                )
            }
        },
        floatingActionButton = {
            when (currentTab) {
                1 -> FloatingActionButton(
                    onClick = { showAddChannelDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar canal")
                }
                2 -> FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Criar grupo")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> FeedScreen(
                    state = state,
                    onSelectGroup = viewModel::selectGroup,
                    onSearchChange = viewModel::setSearchQuery,
                    onToggleHideShorts = viewModel::toggleHideShorts,
                    onToggleHideWatched = viewModel::toggleHideWatched,
                    onOpenVideo = { video ->
                        viewModel.markVideoWatched(video.id)
                        activeVideo = video
                    },
                    onToggleWatched = { video -> viewModel.toggleVideoWatched(video.id) },
                    onGoToChannels = { currentTab = 1 }
                )
                1 -> ChannelsScreen(
                    channels = state.channels,
                    groups = state.groups,
                    onEditGroups = { editingChannel = it },
                    onDeleteChannel = { viewModel.removeChannel(it.id) },
                    onImportCsv = {
                        csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                    },
                    onAddChannel = { showAddChannelDialog = true }
                )
                2 -> GroupsScreen(
                    groups = state.groups,
                    channels = state.channels,
                    onDeleteGroup = { viewModel.deleteGroup(it.id) },
                    onSelectGroupAndOpenFeed = { groupId ->
                        viewModel.selectGroup(groupId)
                        currentTab = 0
                    }
                )
            }
        }
    }

    if (showAddChannelDialog) {
        AddChannelDialog(
            groups = state.groups,
            isLoading = state.isAddingChannel,
            onDismiss = { showAddChannelDialog = false },
            onConfirm = { input, selectedGroups ->
                viewModel.addChannel(input, selectedGroups)
                showAddChannelDialog = false
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name, emoji ->
                viewModel.createGroup(name, emoji)
                showCreateGroupDialog = false
            }
        )
    }

    editingChannel?.let { channel ->
        AssignGroupsDialog(
            channel = channel,
            groups = state.groups,
            onDismiss = { editingChannel = null },
            onConfirm = { newGroupIds ->
                viewModel.updateChannelGroups(channel.id, newGroupIds)
                editingChannel = null
            }
        )
    }
}

@Composable
private fun FeedScreen(
    state: MainUiState,
    onSelectGroup: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleHideShorts: () -> Unit,
    onToggleHideWatched: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onGoToChannels: () -> Unit
) {
    val allowedChannelIds = remember(state.channels, state.selectedGroupId) {
        when (state.selectedGroupId) {
            null -> state.channels.map { it.id }.toSet()
            "ungrouped" -> state.channels.filter { it.groupIds.isEmpty() }.map { it.id }.toSet()
            else -> state.channels.filter { state.selectedGroupId in it.groupIds }.map { it.id }.toSet()
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
                    selected = state.selectedGroupId == null,
                    onClick = { onSelectGroup(null) },
                    label = { Text("Todos") }
                )
            }
            items(state.groups, key = { it.id }) { group ->
                FilterChip(
                    selected = state.selectedGroupId == group.id,
                    onClick = { onSelectGroup(group.id) },
                    label = { Text("${group.emoji} ${group.name}") }
                )
            }
            item {
                FilterChip(
                    selected = state.selectedGroupId == "ungrouped",
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
