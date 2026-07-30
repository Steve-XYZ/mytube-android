plugins {
    // AGP 9+ ships built-in Kotlin support; no separate kotlin.android plugin.
    id("com.android.application") version "9.2.1" apply false
    // Keep the Compose compiler aligned with Kotlin 2.3.10 embedded in AGP 9.2.
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}
