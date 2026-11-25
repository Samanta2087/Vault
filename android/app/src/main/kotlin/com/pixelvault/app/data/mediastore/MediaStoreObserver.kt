package com.pixelvault.app.data.mediastore

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pixelvault.app.data.sync.SyncWorker

class MediaStoreObserver(
    private val context: Context
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var isRegistered = false

    fun register() {
        if (!isRegistered) {
            val contentResolver = context.contentResolver
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            context.contentResolver.unregisterContentObserver(this)
            isRegistered = false
        }
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        // Auto backup trigger: enqueue SyncWorker to process new media
        triggerBackupWorker()
    }

    private fun triggerBackupWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
