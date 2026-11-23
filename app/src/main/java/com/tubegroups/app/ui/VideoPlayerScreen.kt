package com.tubegroups.app.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tubegroups.app.data.YouTubeRssRepository
import com.tubegroups.app.model.Channel
import com.tubegroups.app.model.VideoItem

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    upNextVideos: List<VideoItem>,
    allVideos: List<VideoItem>,
    channels: List<Channel>,
    onSelectVideo: (VideoItem) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val latestVideoId by rememberUpdatedState(video.id)
    val repo = remember { YouTubeRssRepository() }

    var playerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    var playerViewRef by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var loadedVideoId by remember { mutableStateOf<String?>(null) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var exitFullscreenFn by remember { mutableStateOf<(() -> Unit)?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Relacionados, 1=Mesmo canal, 2=Próximos
    var showCastDialog by remember { mutableStateOf(false) }
    var remoteRelated by remember(video.id) { mutableStateOf<List<VideoItem>>(emptyList()) }
    var channelVideosRemote by remember(video.channelId) { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoadingRelated by remember(video.id) { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        isLoadingRelated = true
        remoteRelated = repo.fetchRelatedVideos(video.title, video.id)
        isLoadingRelated = false
    }

    LaunchedEffect(video.channelId) {
        if (video.channelId.startsWith("UC")) {
            channelVideosRemote = repo.fetchChannelVideos(Channel(video.channelId, video.channelTitle))
        }
    }

    val localRelated = remember(video.id, allVideos, channels) {
        findLocalTopicMatches(video, allVideos, channels)
    }

    val relatedList = remember(remoteRelated, localRelated) {
        (remoteRelated + localRelated).distinctBy { it.id }.take(25)
    }

    val sameChannelList = remember(video.id, video.channelId, channelVideosRemote, allVideos) {
        (channelVideosRemote + allVideos.filter { it.channelId == video.channelId })
            .filter { it.id != video.id }
            .distinctBy { it.id }
    }

    val currentTabList = when (selectedTab) {
        0 -> relatedList
        1 -> sameChannelList
        else -> upNextVideos
    }

    BackHandler {
        if (fullscreenView != null) {
            exitFullscreenFn?.invoke()
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fullscreenView?.let { v ->
                (activity?.window?.decorView as? ViewGroup)?.removeView(v)
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            playerViewRef?.let { pv ->
                (context as? LifecycleOwner)?.lifecycle?.removeObserver(pv)
                pv.release()
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                Text(
                    text = video.channelTitle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showCastDialog = true }) {
                    Icon(Icons.Default.Cast, contentDescription = "Transmitir para TV")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("YouTube")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Player nativo 16:9
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                factory = { ctx ->
                    YouTubePlayerView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        enableAutomaticInitialization = false
                        (ctx as? LifecycleOwner)?.lifecycle?.addObserver(this)
                        addFullscreenListener(object : FullscreenListener {
                            override fun onEnterFullscreen(view: View, exitFullscreen: () -> Unit) {
                                fullscreenView = view
                                exitFullscreenFn = exitFullscreen
                                (activity?.window?.decorView as? ViewGroup)?.addView(
                                    view,
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }

                            override fun onExitFullscreen() {
                                fullscreenView?.let { v ->
                                    (activity?.window?.decorView as? ViewGroup)?.removeView(v)
                                }
                                fullscreenView = null
                                exitFullscreenFn = null
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        })
                        val options = IFramePlayerOptions.Builder(ctx).controls(1).fullscreen(1).build()
                        initialize(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                playerRef = youTubePlayer
                                loadedVideoId = latestVideoId
                                youTubePlayer.loadVideo(latestVideoId, 0f)
                            }
                        }, options)
                        playerViewRef = this
                    }
                },
                update = {
                    val player = playerRef
                    if (player != null && loadedVideoId != video.id) {
                        loadedVideoId = video.id
                        player.loadVideo(video.id, 0f)
                    }
                }
            )

            // Título e canal
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.channelTitle} • ${video.publishedLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // 3 Guias
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Relacionados") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Do mesmo canal") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Próximos vídeos") }
                    )
                }
            }

            if (selectedTab == 0 && isLoadingRelated) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }

            if (currentTabList.isEmpty() && !(selectedTab == 0 && isLoadingRelated)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum vídeo encontrado nesta guia.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentTabList, key = { "${selectedTab}_${it.id}" }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVideo(item) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(136.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.channelTitle} • ${item.publishedLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCastDialog) {
        AlertDialog(
            onDismissRequest = { showCastDialog = false },
            title = { Text("Transmitir para a TV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Escolha como enviar este vídeo para a sua TV:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showCastDialog = false
                            try {
                                context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    ) {
                        Text("Espelhar tela do celular na TV")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showCastDialog = false
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
                        }
                    ) {
                        Text("Abrir no YouTube (Cast nativo)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCastDialog = false }) { Text("Fechar") }
            }
        )
    }
}

private fun findLocalTopicMatches(
    current: VideoItem,
    allVideos: List<VideoItem>,
    channels: List<Channel>
): List<VideoItem> {
    val stopWords = setOf("para", "como", "sobre", "entre", "agora", "isso", "este", "esta", "muito", "mais", "aqui", "onde")
    val keywords = current.title.lowercase()
        .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
        .split(Regex("""\s+"""))
        .filter { it.length >= 4 && it !in stopWords }
        .toSet()

    val currentGroups = channels.firstOrNull { it.id == current.channelId }?.groupIds?.toSet().orEmpty()
    val channelGroupMap = channels.associate { it.id to it.groupIds.toSet() }

    return allVideos
        .asSequence()
        .filter { it.id != current.id }
        .map { candidate ->
            val words = candidate.title.lowercase()
            var score = keywords.count { words.contains(it) } * 3
            val candGroups = channelGroupMap[candidate.channelId].orEmpty()
            if (currentGroups.any { it in candGroups }) score += 2
            candidate to score
        }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(15)
        .toList()
}
