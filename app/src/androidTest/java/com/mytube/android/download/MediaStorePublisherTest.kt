package com.mytube.android.download

import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaStorePublisherTest {
    @Test
    fun publishesCompletedMediaIntoSharedStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "phase3-publisher-test.mp4")
        val contents = "MyTube Phase 3".encodeToByteArray()
        source.writeBytes(contents)
        val publisher = MediaStorePublisher(context)
        val published = publisher.publish(source)

        try {
            assertEquals("video/mp4", published.mimeType)
            context.contentResolver.query(
                published.uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )!!.use { cursor ->
                cursor.moveToFirst()
                assertEquals(source.name, cursor.getString(0))
            }
            val stored = context.contentResolver
                .openInputStream(published.uri)!!
                .use { it.readBytes() }
            assertArrayEquals(contents, stored)
        } finally {
            publisher.delete(published.uri)
            source.delete()
        }
    }
}
