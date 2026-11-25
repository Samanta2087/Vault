package com.pixelvault.app.ui.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.pixelvault.app.security.AppLockManager
import com.pixelvault.app.security.BiometricHelper
import com.pixelvault.app.security.BruteForceGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockMode {
    PIN,
    PATTERN
}

class LockViewModel(
    private val appLockManager: AppLockManager,
    private val biometricHelper: BiometricHelper,
    private val bruteForceGuard: BruteForceGuard
) : ViewModel() {

    private val _lockMode = MutableStateFlow(LockMode.PIN)
    val lockMode: StateFlow<LockMode> = _lockMode.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _isLockedOut = MutableStateFlow(false)
    val isLockedOut: StateFlow<Boolean> = _isLockedOut.asStateFlow()

    private val _lockoutSeconds = MutableStateFlow(0)
    val lockoutSeconds: StateFlow<Int> = _lockoutSeconds.asStateFlow()

    fun setLockMode(mode: LockMode) {
        _lockMode.value = mode
        _hasError.value = false
    }

    fun canUseBiometric(): Boolean = biometricHelper.canAuthenticate()

    fun submitPin(pin: String, onUnlocked: () -> Unit) {
        if (bruteForceGuard.isLockedOut("app")) {
            updateLockout()
            return
        }

        val success = appLockManager.unlockAppWithPin(pin)
        if (success) {
            _hasError.value = false
            onUnlocked()
        } else {
            _hasError.value = true
            updateLockout()
        }
    }

    fun submitPattern(dots: List<Int>, onUnlocked: () -> Unit) {
        if (bruteForceGuard.isLockedOut("app")) {
            updateLockout()
            return
        }

        val success = appLockManager.unlockAppWithPattern(dots)
        if (success) {
            _hasError.value = false
            onUnlocked()
        } else {
            _hasError.value = true
            updateLockout()
        }
    }

    fun authenticateBiometric(activity: FragmentActivity, onUnlocked: () -> Unit) {
        biometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                appLockManager.unlockAppWithBiometric()
                onUnlocked()
            },
            onError = {
                _hasError.value = true
            }
        )
    }

    private fun updateLockout() {
        val locked = bruteForceGuard.isLockedOut("app")
        _isLockedOut.value = locked
        if (locked) {
            _lockoutSeconds.value = bruteForceGuard.getRemainingLockoutSeconds("app")
        }
    }
}
