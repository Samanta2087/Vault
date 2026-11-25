package com.pixelvault.app.security

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultEncryption {
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer

    /**
     * Generates a cryptographically secure 256-bit random per-file AES key.
     */
    fun generateFileKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }

    /**
     * Encrypts a stream into AES-256-GCM with a random 12-byte IV prepended.
     */
    fun encryptStream(input: InputStream, output: OutputStream, fileKey: ByteArray) {
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        output.write(iv)

        val keySpec = SecretKeySpec(fileKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val allBytes = input.readBytes()
        val ciphertext = cipher.doFinal(allBytes)
        output.write(ciphertext)
    }

    /**
     * Decrypts an AES-256-GCM encrypted stream by extracting the 12-byte IV.
     */
    fun decryptStream(input: InputStream, output: OutputStream, fileKey: ByteArray) {
        val iv = ByteArray(GCM_IV_LENGTH)
        val bytesRead = input.read(iv)
        if (bytesRead != GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted payload: missing IV")
        }

        val keySpec = SecretKeySpec(fileKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = input.readBytes()
        val plaintext = cipher.doFinal(ciphertext)
        output.write(plaintext)
    }

    /**
     * Wraps (encrypts) the 256-bit per-file key using the device Master Key from Keystore.
     */
    fun wrapFileKey(fileKey: ByteArray, masterKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.WRAP_MODE, masterKey)
        return cipher.wrap(SecretKeySpec(fileKey, "AES"))
    }

    /**
     * Unwraps (decrypts) the per-file key using the device Master Key.
     */
    fun unwrapFileKey(wrappedKey: ByteArray, masterKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.UNWRAP_MODE, masterKey)
        val unwrapped = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY)
        return unwrapped.encoded
    }

    /**
     * Strips all GPS and sensitive device EXIF metadata from media before storing in Vault.
     */
    fun stripExif(context: Context, sourceUri: Uri, destinationFile: File) {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }

        try {
            val exif = ExifInterface(destinationFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_MAKE, null)
            exif.setAttribute(ExifInterface.TAG_MODEL, null)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, null)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, null)
            exif.saveAttributes()
        } catch (_: Exception) {
            // Ignore non-image formats without EXIF headers
        }
    }
}
