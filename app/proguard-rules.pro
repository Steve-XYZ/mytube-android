# yt-dlp maps command output into these models through Jackson reflection.
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Commons Compress registers ZIP extra-field implementations dynamically.
-keep class org.apache.commons.compress.archivers.zip.** { *; }
