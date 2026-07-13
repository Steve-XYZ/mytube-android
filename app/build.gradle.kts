plugins {
    id("com.android.application")
}

android {
    namespace = "com.mytube.spike"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mytube.spike"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1-spike"
        ndk {
            // Real devices + Apple Silicon emulator. Add x86/armeabi-v7a later
            // if we ever care about ancient hardware.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // youtubedl-android executes its bundled python from extracted .so
        // files, so native libs must be extracted, not memory-mapped.
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Published to Maven Central under the junkfood02 group since 0.15
    // (JitPack builds of newer tags are broken).
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
}
