package com.mytube.android.data.download

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mytube.android.data.browser.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadDatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: DownloadRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).build()
        var now = 1_000L
        var id = 0
        repository = RoomDownloadRepository(
            database = database,
            dao = database.downloadDao(),
            now = { now++ },
            newId = { "download-${++id}" },
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun queueIsFifoAndLimitedToTwoConcurrentDownloads() = runTest {
        val first = repository.create(
            "https://example.com/1",
            DownloadFormatPreset.Video720p,
        )
        val second = repository.create(
            "https://example.com/2",
            DownloadFormatPreset.Video720p,
        )
        val third = repository.create(
            "https://example.com/3",
            DownloadFormatPreset.AudioMp3,
        )

        assertTrue(repository.claimForRun(first.id, maxConcurrent = 2))
        assertTrue(repository.claimForRun(second.id, maxConcurrent = 2))
        assertFalse(repository.claimForRun(third.id, maxConcurrent = 2))

        assertTrue(
            repository.complete(
                first.id,
                "content://downloads/1",
                "video/mp4",
                "one.mp4",
            ),
        )
        assertTrue(repository.claimForRun(third.id, maxConcurrent = 2))

        val tasks = repository.observeDownloads().first()
        assertEquals(DownloadState.Completed, tasks.first { it.id == first.id }.state)
        assertEquals(DownloadState.Running, tasks.first { it.id == second.id }.state)
        assertEquals(DownloadState.Running, tasks.first { it.id == third.id }.state)
    }

    @Test
    fun pauseResumeCancelAndDeleteUseExplicitStates() = runTest {
        val task = repository.create(
            "https://example.com/video",
            DownloadFormatPreset.Video480p,
        )

        assertTrue(repository.pause(task.id))
        assertEquals(DownloadState.Paused, repository.get(task.id)?.state)
        assertTrue(repository.resume(task.id))
        assertEquals(DownloadState.Queued, repository.get(task.id)?.state)
        assertTrue(repository.cancel(task.id))
        assertEquals(DownloadState.Cancelled, repository.get(task.id)?.state)
        assertTrue(repository.delete(task.id))
        assertEquals(null, repository.get(task.id))
    }
}
