package com.pixelvault.app.security

import android.os.Build
import android.os.Debug
import java.io.File

object RootDetection {
    enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH }

    data class SecurityAudit(
        val riskLevel: RiskLevel,
        val flags: List<String>
    )

    private val SU_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    private val ROOT_PACKAGES = listOf(
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.topjohnwu.magisk"
    )

    fun audit(): SecurityAudit {
        val flags = mutableListOf<String>()

        // 1. Check for SU binaries
        for (path in SU_PATHS) {
            if (File(path).exists()) {
                flags.add("SU binary found at: $path")
            }
        }

        // 2. Check for test-keys in build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            flags.add("Build tags contain test-keys")
        }

        // 3. Debugger connected check
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            flags.add("Debugger currently attached")
        }

        // 4. Emulator fingerprint check
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        if (fingerprint.startsWith("generic") || fingerprint.startsWith("unknown")
            || model.contains("google_sdk") || model.contains("Emulator") || model.contains("Android SDK built for x86")
            || manufacturer.contains("Genymotion")
        ) {
            flags.add("Device matches emulator profile")
        }

        val riskLevel = when {
            flags.any { it.contains("SU") || it.contains("Magisk") } -> RiskLevel.HIGH
            flags.any { it.contains("Debugger") } -> RiskLevel.MEDIUM
            flags.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        return SecurityAudit(riskLevel, flags)
    }
}
