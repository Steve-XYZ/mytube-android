package com.mytube.android.data.browser

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntry::class, Bookmark::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao
}
