package com.pixelvault.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

class PinManager(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "pixelvault_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasPin(isVault: Boolean = false): Boolean {
        val key = if (isVault) "vault_pin_hash" else "app_pin_hash"
        return prefs.contains(key)
    }

    fun setPin(pin: String, isVault: Boolean = false) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashWithSalt(pin, salt)
        val stored = "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"

        val key = if (isVault) "vault_pin_hash" else "app_pin_hash"
        prefs.edit().putString(key, stored).apply()
    }

    fun verifyPin(pin: String, isVault: Boolean = false): Boolean {
        val key = if (isVault) "vault_pin_hash" else "app_pin_hash"
        val stored = prefs.getString(key, null) ?: return false

        val parts = stored.split(":")
        if (parts.size != 2) return false

        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
        val computedHash = hashWithSalt(pin, salt)

        return MessageDigest.isEqual(expectedHash, computedHash)
    }

    fun hasPattern(isVault: Boolean = false): Boolean {
        val key = if (isVault) "vault_pattern_hash" else "app_pattern_hash"
        return prefs.contains(key)
    }

    fun setPattern(dots: List<Int>, isVault: Boolean = false) {
        setPin(dots.joinToString(","), isVault)
    }

    fun verifyPattern(dots: List<Int>, isVault: Boolean = false): Boolean {
        return verifyPin(dots.joinToString(","), isVault)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun hashWithSalt(input: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(input.toByteArray(Charsets.UTF_8))
    }
}
