package com.example.vault.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.vault.ui.viewer.ViewerViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.weight
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@Composable
fun ViewerScreen(
    viewModel: ViewerViewModel,
    onClose: () -> Unit
) {
    val media by viewModel.currentMedia.collectAsState()
    val index by viewModel.currentIndex.collectAsState()
    val viewedCount by viewModel.viewedCount.collectAsState()
    // Intercept system back to close viewer (prevent going back to previous items)
    BackHandler {
        onClose()
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            media?.let { item ->
                // Decrypt the file on demand
                val fileState = produceState<File?>(initialValue = null, item) {
                    value = viewModel.decryptCurrent()
                }
                val decrypted = fileState.value
                if (decrypted != null) {
                    if (item.mimeType.startsWith("video")) {
                        // Video playback using ExoPlayer
                        val exoPlayer = rememberExoPlayer(uri = Uri.fromFile(decrypted))
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Image display using Coil
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Uri.fromFile(decrypted))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(text = "Decrypting...")
                }
            } ?: run {
                Text(text = "No more items")
            }
        }
        Button(
            onClick = {
                if (media != null) {
                    viewModel.next()
                } else {
                    onClose()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(text = if (media != null) "Next" else "Finish")
        }
    }
}

@Composable
private fun rememberExoPlayer(uri: Uri): ExoPlayer {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    return exoPlayer
}