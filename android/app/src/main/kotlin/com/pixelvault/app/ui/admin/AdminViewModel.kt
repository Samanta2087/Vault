package com.pixelvault.app.ui.admin

import androidx.lifecycle.ViewModel
import com.pixelvault.app.domain.model.StorageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel : ViewModel() {

    private val _stats = MutableStateFlow(
        StorageStats(
            totalMedia = 2418,
            totalUsers = 3,
            storageUsedBytes = 13_800_000_000L, // ~13.8 GB
            storageLimitBytes = 100_000_000_000L // 100 GB
        )
    )
    val stats: StateFlow<StorageStats> = _stats.asStateFlow()

    private val _serverUrl = MutableStateFlow("https://photos.yourdomain.com")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _wifiOnly = MutableStateFlow(true)
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _chargingOnly = MutableStateFlow(false)
    val chargingOnly: StateFlow<Boolean> = _chargingOnly.asStateFlow()

    fun toggleWifiOnly(value: Boolean) {
        _wifiOnly.value = value
    }

    fun toggleChargingOnly(value: Boolean) {
        _chargingOnly.value = value
    }
}
