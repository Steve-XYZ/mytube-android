# Keep debug stack traces readable while shrinking unused code and resources.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# The instrumented test runner shares the app's Kotlin runtime.
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.room.RoomDatabase { *; }
-keep interface androidx.sqlite.db.SupportSQLiteOpenHelper { *; }
-keep interface androidx.sqlite.db.SupportSQLiteDatabase { *; }
-keep class androidx.datastore.preferences.core.PreferenceDataStoreFactory { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep interface androidx.compose.ui.node.RootForTest { *; }
-keep class androidx.compose.ui.semantics.** { *; }

# Compile-time-only Error Prone annotations reference the JDK model API.
-dontwarn javax.lang.model.element.Modifier
