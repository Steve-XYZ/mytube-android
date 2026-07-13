# MyTube Android

Android version of [MyTube](https://github.com/Steve-XYZ/mytube).

## Status: Fase 0 — feasibility spike

`app/` is a **disposable spike** that validates the core bet: yt-dlp
(via [youtubedl-android](https://github.com/yausername/youtubedl-android))
plus ffmpeg running on Android, downloading from the platforms MyTube targets,
with runtime yt-dlp updates.

What it proves:
- yt-dlp + embedded Python init on device
- runtime update of yt-dlp (stable channel)
- metadata extraction (`Get info`)
- download with format selection + ffmpeg merge (`bv*+ba` → mp4)
- share-target (`ACTION_SEND`) entry point

### Run

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# manual: open "MyTube Spike", paste URL, Download
# scripted:
adb shell am start -n com.mytube.spike/.MainActivity --es url "https://www.youtube.com/watch?v=aqz-KE-bpKQ" --ez autodl true
adb logcat -s SPIKE
```

Downloads land in `/sdcard/Android/data/com.mytube.spike/files/downloads/`.

The real app (Fase 1+) will be scaffolded in this repo once the spike passes;
see the plan in the desktop repo discussion.
