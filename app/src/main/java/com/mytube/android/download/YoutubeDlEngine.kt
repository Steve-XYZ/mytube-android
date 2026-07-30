package com.mytube.android.download

import android.content.Context
import com.mytube.android.data.download.DownloadFormatPreset
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class DownloadedMedia(
    val file: File,
    val title: String,
)

class YoutubeDlEngine(
    context: Context,
    private val updateStore: YtDlpUpdateStore = YtDlpUpdateStore(context),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val initializationMutex = Mutex()
    @Volatile
    private var initialized = false

    suspend fun download(
        id: String,
        sourceUrl: String,
        formatPreset: DownloadFormatPreset,
        outputDirectory: File,
        onMetadata: suspend (title: String) -> Unit,
        onProgress: (progress: Int, etaSeconds: Long?) -> Unit,
    ): DownloadedMedia = withContext(Dispatchers.IO) {
        ensureReady()
        prepareDirectory(outputDirectory)

        val videoInfo = YoutubeDL.getInstance().getInfo(
            YoutubeDLRequest(sourceUrl).apply {
                addOption("--no-playlist")
            },
        )
        val title = videoInfo.title?.takeIf(String::isNotBlank) ?: "Download"
        onMetadata(title)

        val request = createDownloadRequest(sourceUrl, formatPreset, outputDirectory)

        YoutubeDL.getInstance().execute(request, id) { progress, etaSeconds, _ ->
            onProgress(
                progress.toInt().coerceIn(0, 100),
                etaSeconds.takeIf { it >= 0 },
            )
        }

        val result = outputDirectory.listFiles()
            ?.filter(File::isFile)
            ?.filterNot { it.name.endsWith(".part") || it.name.endsWith(".ytdl") }
            ?.maxByOrNull(File::lastModified)
            ?: error("yt-dlp completed without producing a media file")

        DownloadedMedia(file = result, title = title)
    }

    fun cancel(id: String) {
        if (initialized) {
            YoutubeDL.getInstance().destroyProcessById(id)
        }
    }

    suspend fun version(): String = withContext(Dispatchers.IO) {
        ensureReady()
        YoutubeDL.getInstance().version(appContext) ?: "unknown"
    }

    private suspend fun ensureReady() {
        initializationMutex.withLock {
            if (!initialized) {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().init(appContext)
                    FFmpeg.getInstance().init(appContext)
                }
                initialized = true
            }

            val timestamp = now()
            if (timestamp - updateStore.lastSuccessfulUpdateAt() >= UpdateIntervalMs) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        YoutubeDL.getInstance().updateYoutubeDL(
                            appContext,
                            YoutubeDL.UpdateChannel.STABLE,
                        )
                    }
                }.onSuccess {
                    updateStore.recordSuccessfulUpdate(timestamp)
                }
            }
        }
    }

    private fun prepareDirectory(directory: File) {
        if (directory.exists()) {
            directory.listFiles()?.forEach(File::deleteRecursively)
        } else {
            check(directory.mkdirs()) { "Unable to create temporary download directory" }
        }
    }

    private companion object {
        const val UpdateIntervalMs = 24 * 60 * 60 * 1_000L
    }
}

internal fun createDownloadRequest(
    sourceUrl: String,
    formatPreset: DownloadFormatPreset,
    outputDirectory: File,
): YoutubeDLRequest = YoutubeDLRequest(sourceUrl).apply {
    addOption(
        "-o",
        "${outputDirectory.absolutePath}/%(title).80s [%(id)s].%(ext)s",
    )
    addOption("-f", formatPreset.formatSelector)
    addOption("--no-playlist")
    addOption("--restrict-filenames")
    addOption("--no-mtime")
    addOption("--newline")
    if (formatPreset.audioOnly) {
        addOption("--extract-audio")
        addOption("--audio-format", "mp3")
        addOption("--audio-quality", "0")
    } else {
        addOption("--merge-output-format", "mp4")
    }
}
