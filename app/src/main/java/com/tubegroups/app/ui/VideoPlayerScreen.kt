package com.tubegroups.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.tubegroups.app.model.VideoItem

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    upNextVideos: List<VideoItem>,
    onSelectVideo: (VideoItem) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        if (fullscreenView != null) {
            fullscreenCallback?.onCustomViewHidden()
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                    Text(
                        text = video.channelTitle,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.url))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("YouTube")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Player 16:9 embutido
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(
                                    view: View?,
                                    callback: CustomViewCallback?
                                ) {
                                    fullscreenView = view
                                    fullscreenCallback = callback
                                }

                                override fun onHideCustomView() {
                                    fullscreenView = null
                                    fullscreenCallback = null
                                }
                            }
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        if (webView.tag != video.id) {
                            webView.tag = video.id
                            webView.loadDataWithBaseURL(
                                "https://tubegroups.app",
                                buildEmbedHtml(video.id),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    }
                )

                // Informações do vídeo atual
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${video.channelTitle} • ${video.publishedLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                HorizontalDivider(color = Color(0xFF2A2A2A))

                // Próximos vídeos para continuar assistindo sem sair do player
                if (upNextVideos.isNotEmpty()) {
                    Text(
                        text = "Próximos vídeos",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(upNextVideos, key = { it.id }) { item ->
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

            // Tela cheia quando o usuário toca no ícone de expandir do player
            if (fullscreenView != null) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            val v = fullscreenView
                            if (v != null) {
                                (v.parent as? ViewGroup)?.removeView(v)
                                addView(
                                    v,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun buildEmbedHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                iframe { width: 100%; height: 100%; border: 0; }
            </style>
        </head>
        <body>
            <iframe
                src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"
                referrerpolicy="strict-origin-when-cross-origin"
                allow="autoplay; encrypted-media; fullscreen; picture-in-picture"
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()
}
