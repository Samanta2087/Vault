package com.pixelvault.app.ui.lock

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelvault.app.ui.components.GlassBottomNav
import com.pixelvault.app.ui.components.MediaThumbnail

/**
 * Secure Vault Screen.
 * - Always enforces FLAG_SECURE (no screenshots, recents preview blanked).
 * - Protected by separate PIN / Pattern / Biometric authentication.
 * - Media stored in Vault is completely excluded from main timeline, search, MediaStore, and sync.
 */
@Composable
fun VaultScreen(
    vaultViewModel: VaultViewModel,
    lockViewModel: LockViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Strict FLAG_SECURE: blocks screen capture while inside the Secure Vault
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val isVaultUnlocked by vaultViewModel.isVaultUnlocked.collectAsState()
    val vaultItems by vaultViewModel.vaultItems.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            vaultViewModel.importMediaToVault(uri.toString())
        }
    }

    if (!isVaultUnlocked) {
        // Vault Lock Screen Gate
        LockScreen(
            viewModel = lockViewModel,
            onUnlocked = { vaultViewModel.unlockVault("temp_ok") },
            isVault = true
        )
    } else {
        // Vault Media Grid
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 80.dp, bottom = 110.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(vaultItems) { media ->
                    MediaThumbnail(
                        media = media,
                        onClick = { /* Decrypt and show in viewer */ },
                        onLongClick = { vaultViewModel.removeFromVault(media.id) }
                    )
                }
            }

            // Top Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "  Secure Vault",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Floating Import FAB
            FloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add to Vault", tint = Color.Black)
            }

            // Floating Liquid Glass Bottom Nav
            GlassBottomNav(
                currentRoute = "vault",
                onNavigate = onNavigate,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
