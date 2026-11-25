package com.pixelvault.app.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelvault.app.domain.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewerViewModel : ViewModel() {

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    fun setMediaList(list: List<MediaItem>, initialIndex: Int) {
        _mediaList.value = list
        _currentIndex.value = initialIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    }

    fun onPageChanged(index: Int) {
        _currentIndex.value = index
    }

    fun toggleFavorite(mediaId: String) {
        viewModelScope.launch {
            // Toggle favorite in repository
        }
    }

    fun deleteCurrent(onDeleted: () -> Unit) {
        viewModelScope.launch {
            // Delete media
            onDeleted()
        }
    }
}
