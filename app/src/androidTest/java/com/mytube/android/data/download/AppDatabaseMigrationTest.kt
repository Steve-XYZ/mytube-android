package com.mytube.android.data.download

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mytube.android.data.browser.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun deleteDatabase() {
        context.deleteDatabase(DatabaseName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(DatabaseName)
    }

    @Test
    fun migration1To2PreservesBrowserDataAndCreatesDownloadQueue() {
        context.openOrCreateDatabase(DatabaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS history (
                    url TEXT NOT NULL,
                    title TEXT NOT NULL,
                    visitedAt INTEGER NOT NULL,
                    visitCount INTEGER NOT NULL,
                    PRIMARY KEY(url)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS bookmarks (
                    url TEXT NOT NULL,
                    title TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(url)
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf(Version1IdentityHash),
            )
            database.execSQL(
                """
                INSERT INTO history(url, title, visitedAt, visitCount)
                VALUES('https://example.com', 'Example', 1, 2)
                """.trimIndent(),
            )
            database.version = 1
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseName)
            .addMigrations(AppDatabase.Migration1To2, AppDatabase.Migration2To3)
            .build()
        try {
            val sqlite = database.openHelper.readableDatabase
            sqlite.query("SELECT COUNT(*) FROM history").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            sqlite.query("SELECT COUNT(*) FROM downloads").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migration2To3AddsAnOptionalImageReferer() {
        context.openOrCreateDatabase(DatabaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS history (
                    url TEXT NOT NULL, title TEXT NOT NULL, visitedAt INTEGER NOT NULL,
                    visitCount INTEGER NOT NULL, PRIMARY KEY(url)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS bookmarks (
                    url TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL,
                    PRIMARY KEY(url)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE downloads (
                    id TEXT NOT NULL, sourceUrl TEXT NOT NULL, title TEXT NOT NULL,
                    formatPreset TEXT NOT NULL, state TEXT NOT NULL, progress INTEGER NOT NULL,
                    etaSeconds INTEGER, outputUri TEXT, outputMimeType TEXT, fileName TEXT,
                    errorMessage TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX index_downloads_state ON downloads(state)")
            database.execSQL("CREATE INDEX index_downloads_createdAt ON downloads(createdAt)")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf(Version2IdentityHash),
            )
            database.execSQL(
                """
                INSERT INTO downloads(
                    id, sourceUrl, title, formatPreset, state, progress, createdAt, updatedAt
                ) VALUES('one', 'https://example.com/video', 'Video', 'Video720p', 'Completed', 100, 1, 2)
                """.trimIndent(),
            )
            database.version = 2
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, DatabaseName)
            .addMigrations(AppDatabase.Migration2To3)
            .build()
        try {
            database.openHelper.readableDatabase
                .query("SELECT refererUrl FROM downloads WHERE id = 'one'")
                .use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(true, cursor.isNull(0))
                }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DatabaseName = "migration-downloads-test"
        const val Version1IdentityHash = "ddd83a17e2e5ffc87195027ccd414083"
        const val Version2IdentityHash = "2dbfab999e5317197979aea64fbec038"
    }
}
