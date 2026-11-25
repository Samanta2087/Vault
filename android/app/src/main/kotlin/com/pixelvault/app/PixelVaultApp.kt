package com.pixelvault.app

import android.app.Application
import com.pixelvault.app.data.mediastore.MediaStoreObserver
import com.pixelvault.app.data.prefs.UserPreferences
import com.pixelvault.app.security.AppLockManager
import com.pixelvault.app.security.BiometricHelper
import com.pixelvault.app.security.BruteForceGuard
import com.pixelvault.app.security.KeystoreHelper
import com.pixelvault.app.security.PinManager

class PixelVaultApp : Application() {

    val pinManager by lazy { PinManager(this) }
    val bruteForceGuard by lazy { BruteForceGuard() }
    val biometricHelper by lazy { BiometricHelper(this) }
    val appLockManager by lazy { AppLockManager(pinManager, bruteForceGuard) }
    val mediaStoreObserver by lazy { MediaStoreObserver(this) }
    val userPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Register ContentObserver to track new camera photos and videos automatically
        mediaStoreObserver.register()
    }

    companion object {
        lateinit var instance: PixelVaultApp
            private set
    }
}
