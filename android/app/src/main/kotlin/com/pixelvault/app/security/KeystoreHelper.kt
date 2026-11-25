package com.pixelvault.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DEFAULT_MASTER_ALIAS = "pixelvault_master_key"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun getOrCreateMasterKey(alias: String = DEFAULT_MASTER_ALIAS): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        // Generate AES-256-GCM key. First attempt StrongBox (dedicated hardware security chip)
        return try {
            generateKey(alias, useStrongBox = true)
        } catch (e: StrongBoxUnavailableException) {
            // Fallback to standard TEE (Trusted Execution Environment)
            generateKey(alias, useStrongBox = false)
        }
    }

    private fun generateKey(alias: String, useStrongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (useStrongBox) {
            specBuilder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(specBuilder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM from Android Keystore.
     * Returns: 12-byte IV + Ciphertext.
     */
    fun encrypt(plaintext: ByteArray, alias: String = DEFAULT_MASTER_ALIAS): ByteArray {
        val key = getOrCreateMasterKey(alias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /**
     * Decrypts combined IV + Ciphertext using AES-256-GCM from Android Keystore.
     */
    fun decrypt(encryptedData: ByteArray, alias: String = DEFAULT_MASTER_ALIAS): ByteArray {
        val key = getOrCreateMasterKey(alias)
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }

    fun deleteKey(alias: String = DEFAULT_MASTER_ALIAS) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }
}
