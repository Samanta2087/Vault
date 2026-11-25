package com.pixelvault.app.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelvault.app.domain.model.MediaItem
import com.pixelvault.app.ui.components.GlassBottomNav
import com.pixelvault.app.ui.components.GlassSurface
import com.pixelvault.app.ui.components.GlassTopBar
import com.pixelvault.app.ui.components.MediaThumbnail

/**
 * Main Photos Timeline Screen.
 * - Exactly reproduces user reference screenshot:
 *   * Large "Photos" heading with Search, Select, and More action icons.
 *   * Date grouping: "Today", "Yesterday", Month Year headers.
 *   * 3-column media grid with pinch-to-zoom (2 to 5 columns).
 *   * Floating Liquid Glass bottom navigation bar.
 *   * Multi-select mode with floating glass batch-actions bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onNavigate: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.timelineItems.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val columns by viewModel.gridColumns.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val gridState = rememberLazyGridState()
    var scaleFactor by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Pinch-to-zoom gesture to change columns (2 to 5)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scaleFactor *= zoom
                    if (scaleFactor > 1.35f) {
                        viewModel.setGridColumns(columns - 1)
                        scaleFactor = 1f
                    } else if (scaleFactor < 0.75f) {
                        viewModel.setGridColumns(columns + 1)
                        scaleFactor = 1f
                    }
                }
            }
    ) {
        // Main Media Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 90.dp, bottom = 110.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(
                items = items,
                span = { item ->
                    when (item) {
                        is TimelineItem.Header -> GridItemSpan(columns)
                        is TimelineItem.Item -> GridItemSpan(1)
                    }
                }
            ) { item ->
                when (item) {
                    is TimelineItem.Header -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }
                    is TimelineItem.Item -> {
                        MediaThumbnail(
                            media = item.media,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelect(item.media.id)
                                } else {
                                    onMediaClick(item.media)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelect(item.media.id)
                            },
                            isSelected = selectedIds.contains(item.media.id),
                            isSelectionMode = isSelectionMode
                        )
                    }
                }
            }
        }

        // Glass Top Bar
        GlassTopBar(
            title = "Photos",
            isSelectionMode = isSelectionMode,
            selectedCount = selectedIds.size,
            onSearchClick = { onNavigate("search") },
            onSelectClick = { viewModel.toggleSelectionMode() },
            onMenuClick = { /* Open overflow menu */ },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Loading Indicator
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Floating Batch Action Bar in Selection Mode
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 24.dp, end = 24.dp)
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleFavoriteSelected() }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { /* Archive */ }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { viewModel.moveToVaultSelected() }) {
                        Icon(Icons.Default.Lock, contentDescription = "Vault", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.deleteSelected() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Trash", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Floating Liquid Glass Bottom Navigation Bar
        GlassBottomNav(
            currentRoute = "photos",
            onNavigate = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
