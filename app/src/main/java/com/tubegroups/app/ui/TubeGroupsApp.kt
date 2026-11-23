package com.tubegroups.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tubegroups.app.model.Channel
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
                title = { Text("TubeGroups", fontWeight = FontWeight.Bold) },
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
                    onAssignChannelToGroups = { video ->
                        editingChannel = state.channels.firstOrNull { it.id == video.channelId }
                    },
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
            onCreateGroup = { name, emoji -> viewModel.createGroup(name, emoji) },
            onConfirm = { newGroupIds ->
                viewModel.updateChannelGroups(channel.id, newGroupIds)
                editingChannel = null
            }
        )
    }
}
