package com.pixelvault.app.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelvault.app.domain.model.Album
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumsViewModel : ViewModel() {

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        _albums.value = listOf(
            Album(id = "favorites", userId = "u1", name = "Favorites", mediaCount = 42),
            Album(id = "trips", userId = "u1", name = "Travel & Trips", mediaCount = 128),
            Album(id = "documents", userId = "u1", name = "Receipts & Documents", mediaCount = 19),
            Album(id = "archive", userId = "u1", name = "Archive", mediaCount = 8)
        )
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            val newAlbum = Album(
                id = "album_${System.currentTimeMillis()}",
                userId = "u1",
                name = name,
                mediaCount = 0
            )
            _albums.value = _albums.value + newAlbum
        }
    }
}
