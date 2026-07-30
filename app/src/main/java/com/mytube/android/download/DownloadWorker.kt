package com.mytube.android.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mytube.android.MainActivity
import com.mytube.android.MyTubeApplication
import com.mytube.android.data.download.DownloadState
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val application = appContext.applicationContext as MyTubeApplication
    private val repository = application.container.downloadRepository
    private val engine = application.container.youtubeDlEngine
    private val publisher = application.container.mediaStorePublisher
    private var activeDownloadId: String? = null

    override suspend fun doWork(): Result = coroutineScope {
        val downloadId = inputData.getString(DownloadIdKey)
            ?: return@coroutineScope Result.failure()
        activeDownloadId = downloadId
        val task = repository.get(downloadId)
            ?: return@coroutineScope Result.failure()
        currentCoroutineContext().job.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                engine.cancel(downloadId)
            }
        }

        if (!repository.claimForRun(downloadId, MyTubeApplication.MaxConcurrentDownloads)) {
            return@coroutineScope if (repository.get(downloadId)?.state == DownloadState.Queued) {
                Result.retry()
            } else {
                Result.success()
            }
        }
        setForeground(createForegroundInfo(task.title, 0, null))

        val outputDirectory = File(applicationContext.cacheDir, "downloads/$downloadId")
        val progressUpdates = Channel<ProgressUpdate>(Channel.CONFLATED)
        val lastProgress = AtomicInteger(-1)
        val progressJob = launch {
            for (update in progressUpdates) {
                repository.updateProgress(downloadId, update.progress, update.etaSeconds)
                setProgress(
                    workDataOf(
                        ProgressKey to update.progress,
                        EtaSecondsKey to (update.etaSeconds ?: -1L),
                    ),
                )
                setForeground(
                    createForegroundInfo(
                        title = repository.get(downloadId)?.title ?: task.title,
                        progress = update.progress,
                        etaSeconds = update.etaSeconds,
                    ),
                )
            }
        }

        try {
            val downloaded = engine.download(
                id = downloadId,
                sourceUrl = task.sourceUrl,
                formatPreset = task.formatPreset,
                outputDirectory = outputDirectory,
                onMetadata = { title ->
                    repository.updateTitle(downloadId, title)
                },
                onProgress = { progress, etaSeconds ->
                    if (lastProgress.getAndSet(progress) != progress) {
                        progressUpdates.trySend(ProgressUpdate(progress, etaSeconds))
                    }
                },
            )
            progressUpdates.close()
            progressJob.join()

            val published = withContext(Dispatchers.IO) {
                publisher.publish(downloaded.file)
            }
            val completed = repository.complete(
                id = downloadId,
                outputUri = published.uri.toString(),
                outputMimeType = published.mimeType,
                fileName = published.fileName,
            )
            if (!completed) {
                publisher.delete(published.uri)
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            engine.cancel(downloadId)
            throw cancelled
        } catch (cancelled: YoutubeDL.CanceledException) {
            val state = repository.get(downloadId)?.state
            if (state != DownloadState.Paused && state != DownloadState.Cancelled) {
                repository.fail(downloadId, "The download was interrupted.")
            }
            Result.success()
        } catch (error: Throwable) {
            val state = repository.get(downloadId)?.state
            if (state == DownloadState.Running || state == DownloadState.Queued) {
                repository.fail(downloadId, userMessageFor(error))
            }
            Result.failure()
        } finally {
            progressUpdates.close()
            progressJob.cancel()
            outputDirectory.deleteRecursively()
            activeDownloadId = null
        }
    }

    private fun createForegroundInfo(
        title: String,
        progress: Int,
        etaSeconds: Long?,
    ): ForegroundInfo {
        createNotificationChannel()
        val notificationId = NotificationIdBase +
            (activeDownloadId ?: title).hashCode().absoluteValueMod(10_000)
        val openIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationId,
            Intent(applicationContext, DownloadActionReceiver::class.java).apply {
                action = DownloadActionReceiver.CancelAction
                putExtra(DownloadActionReceiver.DownloadIdExtra, activeDownloadId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentText = when {
            progress <= 0 -> "Preparing download"
            etaSeconds != null -> "$progress% · ${etaSeconds}s remaining"
            else -> "$progress%"
        }
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannelId,
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, progress <= 0)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelIntent,
            )
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun createNotificationChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelId,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "MyTube download progress"
            },
        )
    }

    private fun userMessageFor(error: Throwable): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            "network" in message || "connection" in message ||
                "resolve" in message || "timed out" in message ->
                "The network connection failed. Check your connection and retry."
            "unsupported" in message -> "This site or media URL is not supported."
            "private" in message || "sign in" in message || "age" in message ->
                "This media requires sign-in or is not publicly available."
            else -> "yt-dlp could not download this media."
        }
    }

    private data class ProgressUpdate(
        val progress: Int,
        val etaSeconds: Long?,
    )

    companion object {
        const val DownloadIdKey = "download_id"
        const val ProgressKey = "progress"
        const val EtaSecondsKey = "eta_seconds"
        const val DownloadTag = "mytube-download"
        private const val NotificationChannelId = "downloads"
        private const val NotificationIdBase = 20_000
    }
}

private fun Int.absoluteValueMod(divisor: Int): Int =
    if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this) % divisor
