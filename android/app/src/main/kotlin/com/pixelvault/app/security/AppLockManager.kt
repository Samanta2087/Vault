package com.pixelvault.app.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockState {
    UNLOCKED,
    APP_LOCKED,
    VAULT_LOCKED
}

class AppLockManager(
    private val pinManager: PinManager,
    private val bruteForceGuard: BruteForceGuard
) {
    // If PIN is not set yet, default to UNLOCKED so initial launch works seamlessly
    private val _lockState = MutableStateFlow(
        if (pinManager.hasPin(isVault = false)) LockState.APP_LOCKED else LockState.UNLOCKED
    )
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private var backgroundTimestamp = 0L
    private var lockTimeoutMs = 60_000L // Default 1 minute auto-lock

    fun setLockTimeout(timeoutMs: Long) {
        lockTimeoutMs = timeoutMs
    }

    fun onAppForegrounded() {
        if (backgroundTimestamp > 0 && lockTimeoutMs > 0) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            if (elapsed >= lockTimeoutMs && pinManager.hasPin(isVault = false)) {
                _lockState.value = LockState.APP_LOCKED
            }
        }
        backgroundTimestamp = 0L
    }

    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    fun unlockAppWithPin(pin: String): Boolean {
        if (bruteForceGuard.isLockedOut("app")) return false

        val valid = pinManager.verifyPin(pin, isVault = false)
        if (valid) {
            bruteForceGuard.recordSuccess("app")
            _lockState.value = LockState.UNLOCKED
            return true
        } else {
            val attempts = bruteForceGuard.recordFailure("app")
            if (bruteForceGuard.shouldWipeKeys("app")) {
                pinManager.clearAll()
            }
            return false
        }
    }

    fun unlockAppWithPattern(dots: List<Int>): Boolean {
        if (bruteForceGuard.isLockedOut("app")) return false

        val valid = pinManager.verifyPattern(dots, isVault = false)
        if (valid) {
            bruteForceGuard.recordSuccess("app")
            _lockState.value = LockState.UNLOCKED
            return true
        } else {
            bruteForceGuard.recordFailure("app")
            return false
        }
    }

    fun unlockAppWithBiometric() {
        bruteForceGuard.recordSuccess("app")
        _lockState.value = LockState.UNLOCKED
    }

    fun unlockVaultWithPin(pin: String): Boolean {
        if (bruteForceGuard.isLockedOut("vault")) return false

        val valid = pinManager.verifyPin(pin, isVault = true)
        if (valid) {
            bruteForceGuard.recordSuccess("vault")
            return true
        } else {
            bruteForceGuard.recordFailure("vault")
            return false
        }
    }

    fun lockAppNow() {
        _lockState.value = LockState.APP_LOCKED
    }
}
