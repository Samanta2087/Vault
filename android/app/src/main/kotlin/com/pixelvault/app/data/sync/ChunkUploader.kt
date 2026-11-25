package com.pixelvault.app.data.sync

import com.pixelvault.app.data.api.ApiService
import com.pixelvault.app.data.api.UploadInitRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream

class ChunkUploader(
    private val apiService: ApiService
) {
    companion object {
        const val CHUNK_SIZE = 5 * 1024 * 1024 // 5 MB chunks
    }

    suspend fun uploadFile(
        file: File,
        mimeType: String,
        onProgress: (bytesUploaded: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val totalSize = file.length()

        // 1. Initialize resumable chunk upload
        val initRes = apiService.initUpload(
            UploadInitRequest(
                filename = file.name,
                size = totalSize,
                mimeType = mimeType
            )
        )
        if (!initRes.isSuccessful || initRes.body() == null) return@withContext false
        val uploadId = initRes.body()!!.uploadId

        // 2. Stream chunks sequentially
        val buffer = ByteArray(CHUNK_SIZE)
        var offset = 0L

        FileInputStream(file).use { fis ->
            while (offset < totalSize) {
                val bytesToRead = (totalSize - offset).coerceAtMost(CHUNK_SIZE.toLong()).toInt()
                val bytesRead = fis.read(buffer, 0, bytesToRead)
                if (bytesRead <= 0) break

                val chunkBytes = if (bytesRead == CHUNK_SIZE) buffer else buffer.copyOfRange(0, bytesRead)
                val body = chunkBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())

                val endOffset = offset + bytesRead - 1
                val contentRange = "bytes $offset-$endOffset/$totalSize"

                val chunkRes = apiService.uploadChunk(uploadId, contentRange, body)
                if (!chunkRes.isSuccessful) return@withContext false

                offset += bytesRead
                onProgress(offset, totalSize)
            }
        }

        // 3. Complete chunk upload
        val completeRes = apiService.completeUpload(uploadId)
        return@withContext completeRes.isSuccessful
    }
}
