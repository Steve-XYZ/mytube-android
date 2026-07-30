package com.mytube.android.data.browser

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mytube.android.data.download.DownloadDao
import com.mytube.android.data.download.DownloadTask
import com.mytube.android.data.download.DownloadTypeConverters

@Database(
    entities = [HistoryEntry::class, Bookmark::class, DownloadTask::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DownloadTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloads (
                        id TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        title TEXT NOT NULL,
                        formatPreset TEXT NOT NULL,
                        state TEXT NOT NULL,
                        progress INTEGER NOT NULL,
                        etaSeconds INTEGER,
                        outputUri TEXT,
                        outputMimeType TEXT,
                        fileName TEXT,
                        errorMessage TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_downloads_state ON downloads(state)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_downloads_createdAt ON downloads(createdAt)",
                )
            }
        }
    }
}
