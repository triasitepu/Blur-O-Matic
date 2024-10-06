package com.example.bluromatic.data

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.WorkInfo
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.work.WorkManager
import com.example.bluromatic.workers.BlurWorker
import androidx.work.OneTimeWorkRequestBuilder
import com.example.bluromatic.getImageUri
import androidx.work.OneTimeWorkRequest
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import androidx.work.ExistingWorkPolicy
import com.example.bluromatic.IMAGE_MANIPULATION_WORK_NAME
import com.example.bluromatic.TAG_OUTPUT
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.mapNotNull
import androidx.work.Constraints


class WorkManagerBluromaticRepository(context: Context) : BluromaticRepository {

    private var imageUri: Uri = context.getImageUri()
    private val workManager = WorkManager.getInstance(context)

    override val outputWorkInfo: Flow<WorkInfo> =
        workManager.getWorkInfosByTagLiveData(TAG_OUTPUT).asFlow().mapNotNull {
            if (it.isNotEmpty()) it.first() else null
        }

    override fun applyBlur(blurLevel: Int) {
        // Add WorkRequest to Cleanup temporary images
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Create WorkRequest to blur the image
        val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

        blurBuilder.setInputData(createInputDataForWorkRequest(blurLevel, imageUri))

        blurBuilder.setConstraints(constraints) // Add this code

        continuation = continuation.then(blurBuilder.build())

        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .addTag(TAG_OUTPUT)
            .build()
        continuation = continuation.then(save)
        // Start the work
        continuation.enqueue()
    }

    override fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    private fun createInputDataForWorkRequest(blurLevel: Int, imageUri: Uri): Data {
        val builder = Data.Builder()
        builder.putString(KEY_IMAGE_URI, imageUri.toString()).putInt(KEY_BLUR_LEVEL, blurLevel)
        return builder.build()
    }
}