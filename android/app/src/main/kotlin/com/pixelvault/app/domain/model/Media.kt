package com.pixelvault.app.domain.model

data class MediaItem(
    val id: String,
    val userId: String,
    val filename: String,
    val originalName: String,
    val size: Long,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val takenAtEpoch: Long = System.currentTimeMillis(),
    val lat: Double? = null,
    val lng: Double? = null,
    val city: String? = null,
    val country: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isVault: Boolean = false,
    val deletedAtEpoch: Long? = null,
    val uploadStatus: UploadStatus = UploadStatus.READY,
    val localPath: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val durationFormatted: String get() {
        if (durationMs <= 0) return ""
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

enum class UploadStatus {
    PENDING,
    UPLOADING,
    READY,
    FAILED
}

data class Album(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val coverMediaId: String? = null,
    val coverUrl: String? = null,
    val mediaCount: Int = 0,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

data class User(
    val id: String,
    val email: String,
    val name: String?,
    val role: String = "user"
)

data class StorageStats(
    val totalMedia: Int,
    val totalUsers: Int,
    val storageUsedBytes: Long,
    val storageLimitBytes: Long = 100L * 1024 * 1024 * 1024 // 100 GB
)
