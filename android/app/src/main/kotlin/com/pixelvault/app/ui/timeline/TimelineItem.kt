package com.pixelvault.app.ui.timeline

import com.pixelvault.app.domain.model.MediaItem

sealed class TimelineItem {
    data class Header(val title: String, val dateEpoch: Long) : TimelineItem()
    data class Item(val media: MediaItem) : TimelineItem()
}
