package com.pixelvault.app.ui.search

import androidx.lifecycle.ViewModel
import com.pixelvault.app.domain.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<MediaItem>>(emptyList())
    val results: StateFlow<List<MediaItem>> = _results.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        // Triggers full-text / date / location search
    }
}
