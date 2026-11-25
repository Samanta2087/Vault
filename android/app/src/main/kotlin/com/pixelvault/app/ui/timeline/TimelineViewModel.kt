package com.pixelvault.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelvault.app.domain.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TimelineViewModel : ViewModel() {

    private val _timelineItems = MutableStateFlow<List<TimelineItem>>(emptyList())
    val timelineItems: StateFlow<List<TimelineItem>> = _timelineItems.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(3) // Default 3 columns matching screenshot
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadMockData()
    }

    fun setGridColumns(cols: Int) {
        _gridColumns.value = cols.coerceIn(2, 5)
    }

    fun toggleSelectionMode() {
        val next = !_isSelectionMode.value
        _isSelectionMode.value = next
        if (!next) {
            _selectedIds.value = emptySet()
        }
    }

    fun toggleSelect(id: String) {
        _selectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
        if (_selectedIds.value.isEmpty()) {
            _isSelectionMode.value = false
        } else {
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun toggleFavoriteSelected() {
        viewModelScope.launch {
            clearSelection()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _selectedIds.value
            _timelineItems.update { items ->
                items.filterNot { it is TimelineItem.Item && toDelete.contains(it.media.id) }
            }
            clearSelection()
        }
    }

    fun moveToVaultSelected() {
        viewModelScope.launch {
            val toVault = _selectedIds.value
            _timelineItems.update { items ->
                items.filterNot { it is TimelineItem.Item && toVault.contains(it.media.id) }
            }
            clearSelection()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadMockData()
            _isRefreshing.value = false
        }
    }

    private fun loadMockData() {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        val list = mutableListOf<TimelineItem>()

        // Today Group
        list.add(TimelineItem.Header("Today", now))
        for (i in 1..6) {
            list.add(
                TimelineItem.Item(
                    MediaItem(
                        id = "today_$i",
                        userId = "u1",
                        filename = "IMG_TODAY_$i.jpg",
                        originalName = "Screenshot_20261122_$i.jpg",
                        size = 2_400_000L,
                        mimeType = "image/jpeg",
                        width = 1080,
                        height = 1920,
                        takenAtEpoch = now - (i * 3600_000L),
                        thumbnailUrl = "https://picsum.photos/seed/photo$i/400/400"
                    )
                )
            )
        }

        // Yesterday Group
        list.add(TimelineItem.Header("Yesterday", now - oneDay))
        for (i in 7..15) {
            list.add(
                TimelineItem.Item(
                    MediaItem(
                        id = "yesterday_$i",
                        userId = "u1",
                        filename = "IMG_YEST_$i.jpg",
                        originalName = "Photo_20261121_$i.jpg",
                        size = 3_100_000L,
                        mimeType = if (i % 4 == 0) "video/mp4" else "image/jpeg",
                        width = 1080,
                        height = 1920,
                        takenAtEpoch = now - oneDay - (i * 1800_000L),
                        thumbnailUrl = "https://picsum.photos/seed/photo$i/400/400",
                        durationMs = if (i % 4 == 0) 45_000L else 0L
                    )
                )
            )
        }

        _timelineItems.value = list
    }
}
