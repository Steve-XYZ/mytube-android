package com.mytube.android.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class PublishedMedia(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
)

class MediaStorePublisher(
    context: Context,
) {
    private val resolver = context.applicationContext.contentResolver

    fun publish(file: File): PublishedMedia {
        val mimeType = mimeTypeFor(file.extension)
        val collection = collectionFor(mimeType)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/MyTube",
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                put(MediaStore.MediaColumns.DATA, legacyDestination(file.name).absolutePath)
            }
        }
        val uri = checkNotNull(resolver.insert(collection, values)) {
            "Unable to create the destination file"
        }

        try {
            checkNotNull(resolver.openOutputStream(uri, "w")).use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    },
                    null,
                    null,
                )
            }
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }

        return PublishedMedia(
            uri = uri,
            mimeType = mimeType,
            fileName = file.name,
        )
    }

    fun delete(uri: Uri) {
        resolver.delete(uri, null, null)
    }

    @Suppress("DEPRECATION")
    private fun legacyDestination(fileName: String): File {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MyTube",
        )
        check(directory.exists() || directory.mkdirs()) {
            "Unable to create the MyTube downloads directory"
        }

        val requested = File(directory, fileName)
        if (!requested.exists()) return requested
        val stem = requested.nameWithoutExtension
        val extension = requested.extension.takeIf(String::isNotBlank)?.let { ".$it" }.orEmpty()
        var suffix = 2
        var candidate: File
        do {
            candidate = File(directory, "$stem ($suffix)$extension")
            suffix += 1
        } while (candidate.exists())
        return candidate
    }

    private fun collectionFor(mimeType: String): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else if (mimeType.startsWith("audio/")) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
}

internal fun mimeTypeFor(extension: String): String = when (extension.lowercase()) {
    "mp3" -> "audio/mpeg"
    "m4a" -> "audio/mp4"
    "opus" -> "audio/opus"
    "ogg" -> "audio/ogg"
    "wav" -> "audio/wav"
    "webm" -> "video/webm"
    "mkv" -> "video/x-matroska"
    "mov" -> "video/quicktime"
    else -> "video/mp4"
}
