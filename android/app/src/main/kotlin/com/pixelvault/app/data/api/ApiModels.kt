package com.pixelvault.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null,
    @Json(name = "device_name") val deviceName: String? = null
)

@JsonClass(generateAdapter = false)
data class LoginResponse(
    @Json(name = "token") val token: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = false)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: String
)

@JsonClass(generateAdapter = false)
data class MediaItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "original_name") val originalName: String,
    @Json(name = "size") val size: Long,
    @Json(name = "mime_type") val mimeType: String,
    @Json(name = "width") val width: Int = 0,
    @Json(name = "height") val height: Int = 0,
    @Json(name = "taken_at") val takenAt: String? = null,
    @Json(name = "is_favorite") val isFavorite: Boolean = false,
    @Json(name = "is_archived") val isArchived: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = false)
data class MediaPageDto(
    @Json(name = "items") val items: List<MediaItemDto>,
    @Json(name = "next_cursor") val nextCursor: String? = null
)

@JsonClass(generateAdapter = false)
data class UploadInitRequest(
    @Json(name = "filename") val filename: String,
    @Json(name = "size") val size: Long,
    @Json(name = "mime_type") val mimeType: String
)

@JsonClass(generateAdapter = false)
data class UploadInitResponse(
    @Json(name = "upload_id") val uploadId: String,
    @Json(name = "chunk_size") val chunkSize: Long
)

@JsonClass(generateAdapter = false)
data class SignedUrlResponse(
    @Json(name = "url") val url: String,
    @Json(name = "expires_at") val expiresAt: Long
)
