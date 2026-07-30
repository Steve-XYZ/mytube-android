# MyTube Android

Android version of [MyTube](https://github.com/Steve-XYZ/mytube).

## Status: Fase 1 — application foundation

The real application now lives in `app/` and provides:

- a single-activity Jetpack Compose shell
- the MyTube light/dark brand theme
- Navigation 3 destinations for Browser, Downloads, Library, and Settings
- unidirectional UI state held by ViewModels
- unit tests plus a GitHub Actions validation workflow

The Browser and download engine are intentionally placeholders. The minimum
browser lands in Phase 2 and the validated download engine moves into the app
in Phase 3.

## Feasibility spike

`spike/` preserves the disposable Phase 0 prototype that validates yt-dlp
(via [youtubedl-android](https://github.com/yausername/youtubedl-android))
plus ffmpeg running on Android, downloading from the platforms MyTube targets,
with runtime yt-dlp updates.

What it proves:
- yt-dlp + embedded Python init on device
- runtime update of yt-dlp (stable channel)
- metadata extraction (`Get info`)
- download with format selection + ffmpeg merge (`bv*+ba` → mp4)
- share-target (`ACTION_SEND`) entry point

### Build and test the app

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug

adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run the Phase 0 spike

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :spike:assembleDebug
adb install -r spike/build/outputs/apk/debug/spike-debug.apk

# manual: open "MyTube Spike", paste URL, Download
# scripted:
adb shell am start -n com.mytube.spike/.MainActivity --es url "https://www.youtube.com/watch?v=aqz-KE-bpKQ" --ez autodl true
adb logcat -s SPIKE
```

Downloads land in `/sdcard/Android/data/com.mytube.spike/files/downloads/`.
