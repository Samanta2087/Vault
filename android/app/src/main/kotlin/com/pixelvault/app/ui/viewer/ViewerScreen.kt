package com.pixelvault.app.ui.viewer

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pixelvault.app.domain.model.MediaItem
import com.pixelvault.app.ui.components.GlassDialog
import com.pixelvault.app.ui.components.GlassSurface
import kotlinx.coroutines.delay

@Composable
fun ViewerScreen(
    initialMediaId: String,
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaList by viewModel.mediaList.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceAtLeast(0),
        pageCount = { mediaList.size }
    )

    var areControlsVisible by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Auto-hide controls after 3 seconds of inactivity
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            delay(3500)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { areControlsVisible = !areControlsVisible }
                )
            }
    ) {
        if (mediaList.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val media = mediaList[page]

                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = if (scale > 1f) 1f else 2.5f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (media.isVideo) {
                        VideoPlayerComposable(
                            videoUri = media.localPath ?: media.thumbnailUrl ?: "",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = media.localPath ?: media.thumbnailUrl,
                            contentDescription = media.filename,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }
                }
            }
        }

        // Top Glass Navigation Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    val currentMedia = mediaList.getOrNull(pagerState.currentPage)
                    Text(
                        text = currentMedia?.filename ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(onClick = { /* Show EXIF info sheet */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Glass Action Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                cornerRadius = 32.dp
            ) {
                val currentMedia = mediaList.getOrNull(pagerState.currentPage)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Shared from PixelVault")
                            type = "image/*"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Media"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    // Favorite
                    IconButton(onClick = {
                        currentMedia?.let { viewModel.toggleFavorite(it.id) }
                    }) {
                        Icon(
                            imageVector = if (currentMedia?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentMedia?.isFavorite == true) Color.Red else Color.White
                        )
                    }

                    // Move to Vault
                    IconButton(onClick = {
                        // Move to secure vault
                    }) {
                        Icon(Icons.Default.Lock, contentDescription = "Vault", tint = Color.White)
                    }

                    // Delete
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            GlassDialog(
                title = "Move to Trash?",
                message = "This item will be moved to Trash and permanently wiped after 30 days.",
                confirmText = "Move to Trash",
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteCurrent(onDeleted = onBack)
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}
