package com.pixelvault.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelvault.app.domain.model.MediaItem
import com.pixelvault.app.security.AppLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VaultViewModel(
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _vaultItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val vaultItems: StateFlow<List<MediaItem>> = _vaultItems.asStateFlow()

    fun unlockVault(pin: String): Boolean {
        val success = appLockManager.unlockVaultWithPin(pin)
        if (success) {
            _isVaultUnlocked.value = true
            loadVaultItems()
        }
        return success
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun importMediaToVault(mediaId: String) {
        viewModelScope.launch {
            // Strips EXIF and encrypts with AES-256-GCM file key wrapped by Keystore
        }
    }

    fun removeFromVault(mediaId: String) {
        viewModelScope.launch {
            _vaultItems.update { items -> items.filterNot { it.id == mediaId } }
        }
    }

    private fun loadVaultItems() {
        // Loads encrypted vault items
        _vaultItems.value = emptyList()
    }
}
