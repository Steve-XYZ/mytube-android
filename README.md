# MyTube Android

Android version of [MyTube](https://github.com/Steve-XYZ/mytube).

## Status: Phase 2 — minimum browser

The real application lives in `app/` and now provides:

- secure in-app browsing with one `WebView` per tab and up to eight tabs
- address/search normalization with HTTPS-only navigation
- back, forward, reload/stop, tab switching, and bookmarks
- persistent bookmarks and browsing history with Room
- persistent theme and privacy preferences with DataStore
- third-party cookie blocking and hardened WebView file/content access
- a single-activity Jetpack Compose shell with Navigation 3

The Downloads destination remains a placeholder. Phase 3 connects the
validated yt-dlp/ffmpeg engine from the spike to a persistent download queue.

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
  ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug \
  :app:assembleDebugAndroidTest

adb install -r app/build/outputs/apk/debug/app-debug.apk
```

With an emulator or device running:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:connectedDebugAndroidTest
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
