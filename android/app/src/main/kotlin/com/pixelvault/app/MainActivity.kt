package com.pixelvault.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.pixelvault.app.ui.theme.PixelVaultTheme

class MainActivity : FragmentActivity() {

    private val appLockManager by lazy {
        (application as PixelVaultApp).appLockManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PixelVaultTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PixelVaultNavigation(appLockManager = appLockManager)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appLockManager.onAppForegrounded()
    }

    override fun onPause() {
        super.onPause()
        appLockManager.onAppBackgrounded()
    }
}
