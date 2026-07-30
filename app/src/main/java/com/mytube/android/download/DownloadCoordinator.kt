package com.mytube.android.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mytube.android.data.download.DownloadFormatPreset
import com.mytube.android.data.download.DownloadRepository
import com.mytube.android.data.download.DownloadTask

interface DownloadQueueController {
    suspend fun enqueue(
        sourceUrl: String,
        formatPreset: DownloadFormatPreset,
    ): DownloadTask
    suspend fun pause(id: String): Boolean
    suspend fun resume(id: String): Boolean
    suspend fun cancel(id: String): Boolean
    suspend fun delete(id: String): Boolean
}

class DownloadCoordinator(
    context: Context,
    private val repository: DownloadRepository,
    private val engine: YoutubeDlEngine,
    private val workManager: WorkManager = WorkManager.getInstance(context),
) : DownloadQueueController {
    override suspend fun enqueue(
        sourceUrl: String,
        formatPreset: DownloadFormatPreset,
    ): DownloadTask {
        val task = repository.create(sourceUrl, formatPreset)
        schedule(task.id, ExistingWorkPolicy.KEEP)
        return task
    }

    override suspend fun pause(id: String): Boolean {
        val changed = repository.pause(id)
        if (changed) {
            engine.cancel(id)
            workManager.cancelUniqueWork(workName(id))
        }
        return changed
    }

    override suspend fun resume(id: String): Boolean {
        val changed = repository.resume(id)
        if (changed) {
            schedule(id, ExistingWorkPolicy.REPLACE)
        }
        return changed
    }

    override suspend fun cancel(id: String): Boolean {
        val changed = repository.cancel(id)
        if (changed) {
            engine.cancel(id)
            workManager.cancelUniqueWork(workName(id))
        }
        return changed
    }

    override suspend fun delete(id: String): Boolean {
        workManager.cancelUniqueWork(workName(id))
        return repository.delete(id)
    }

    suspend fun restoreQueued() {
        repository.getQueued().forEach { task ->
            schedule(task.id, ExistingWorkPolicy.KEEP)
        }
    }

    private fun schedule(id: String, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.DownloadIdKey to id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag(DownloadWorker.DownloadTag)
            .addTag("download:$id")
            .build()

        workManager.enqueueUniqueWork(workName(id), policy, request)
    }

    private fun workName(id: String): String = "download-$id"
}
