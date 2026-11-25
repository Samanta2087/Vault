package com.pixelvault.app.ui.lock

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.pixelvault.app.ui.components.GlassSurface

/**
 * High-Security Lock Screen.
 * - Sets Window FLAG_SECURE: Prevents screenshots, video recordings, and obscures recent-apps thumbnail.
 * - Supports PIN and 3x3 Pattern Lock (min 5 dots).
 * - BiometricPrompt integration.
 * - Brute-force exponential lockout protection with timer.
 */
@Composable
fun LockScreen(
    viewModel: LockViewModel,
    onUnlocked: () -> Unit,
    isVault: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Set WindowManager.LayoutParams.FLAG_SECURE on locked screens
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!isVault) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    val lockMode by viewModel.lockMode.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val isLockedOut by viewModel.isLockedOut.collectAsState()
    val lockoutSeconds by viewModel.lockoutSeconds.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Lock Branding Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isVault) "Secure Vault" else "PixelVault",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = if (isLockedOut) "Too many attempts. Try again in ${lockoutSeconds}s" else "Enter credentials to proceed",
                fontSize = 14.sp,
                color = if (isLockedOut) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Switch between PIN and Pattern
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = { viewModel.setLockMode(LockMode.PIN) },
                    colors = if (lockMode == LockMode.PIN) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.5f))
                ) {
                    Text("PIN Code", fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = { viewModel.setLockMode(LockMode.PATTERN) },
                    colors = if (lockMode == LockMode.PATTERN) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.5f))
                ) {
                    Text("Pattern Grid", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PIN or Pattern Composable
            if (lockMode == LockMode.PIN) {
                PinEntryComposable(
                    pinLength = 6,
                    hasError = hasError,
                    onPinComplete = { pin -> viewModel.submitPin(pin, onUnlocked) }
                )
            } else {
                PatternLockComposable(
                    minDots = 5,
                    hasError = hasError,
                    onPatternComplete = { dots -> viewModel.submitPattern(dots, onUnlocked) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Biometric Option
            if (viewModel.canUseBiometric() && context is FragmentActivity) {
                IconButton(
                    onClick = { viewModel.authenticateBiometric(context, onUnlocked) },
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}
