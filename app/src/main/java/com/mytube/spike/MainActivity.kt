package com.mytube.spike

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fase 0 spike: prove that yt-dlp (via youtubedl-android) + ffmpeg can run on
 * Android and download from the platforms MyTube targets. Disposable code —
 * everything is logged to the screen and to logcat (tag SPIKE) so runs can be
 * driven and verified over adb:
 *
 *   adb shell am start -n com.mytube.spike/.MainActivity --es url "<URL>" --ez autodl true
 *   adb logcat -s SPIKE
 */
class MainActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val initialized = AtomicBoolean(false)

    private lateinit var urlInput: EditText
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var infoButton: Button
    private lateinit var downloadButton: Button

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()

        urlInput.setText(urlFromIntent(intent) ?: "https://www.youtube.com/watch?v=aqz-KE-bpKQ")

        val autoDownload = intent.getBooleanExtra("autodl", false)
        executor.execute {
            initEngines()
            if (autoDownload) {
                runOnUiThread { downloadButton.performClick() }
            }
        }

        infoButton.setOnClickListener { withEngine { fetchInfo(currentUrl()) } }
        downloadButton.setOnClickListener { withEngine { download(currentUrl()) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        urlFromIntent(intent)?.let { urlInput.setText(it) }
    }

    private fun urlFromIntent(intent: Intent): String? =
        intent.getStringExtra("url")
            ?: intent.getStringExtra(Intent.EXTRA_TEXT).takeIf { intent.action == Intent.ACTION_SEND }

    private fun currentUrl() = urlInput.text.toString().trim()

    /** Queue work behind engine init so buttons are safe to mash early. */
    private fun withEngine(work: () -> Unit) {
        executor.execute {
            if (!initialized.get()) {
                log("engines not ready yet; queued behind init")
            }
            work()
        }
    }

    // ==================== yt-dlp engine ====================

    private fun initEngines() {
        try {
            val t0 = System.currentTimeMillis()
            log("init: YoutubeDL...")
            YoutubeDL.getInstance().init(this)
            log("init: FFmpeg...")
            FFmpeg.getInstance().init(this)
            log("init done in ${System.currentTimeMillis() - t0}ms")
            log("yt-dlp version: ${YoutubeDL.getInstance().version(this)}")

            // Same lesson as desktop: stale yt-dlp breaks YouTube. Validate
            // the runtime updater as part of the spike.
            log("updating yt-dlp (stable channel)...")
            val status = YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel.STABLE)
            log("update status: $status, version now: ${YoutubeDL.getInstance().version(this)}")
            initialized.set(true)
            log("READY")
        } catch (e: Throwable) {
            log("INIT FAILED: ${Log.getStackTraceString(e)}")
        }
    }

    private fun fetchInfo(url: String) {
        if (url.isEmpty()) return log("no url")
        try {
            log("getInfo: $url")
            val t0 = System.currentTimeMillis()
            val info = YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url))
            log("title: ${info.title}")
            log("uploader: ${info.uploader}, duration: ${info.duration}s")
            log("getInfo took ${System.currentTimeMillis() - t0}ms")
        } catch (e: Throwable) {
            log("GETINFO FAILED: ${Log.getStackTraceString(e)}")
        }
    }

    private fun download(url: String) {
        if (url.isEmpty()) return log("no url")
        val outDir = File(getExternalFilesDir(null), "downloads").apply { mkdirs() }
        try {
            log("download: $url")
            log("into: ${outDir.absolutePath}")
            val request = YoutubeDLRequest(url).apply {
                addOption("-o", "${outDir.absolutePath}/%(title).80s.%(ext)s")
                // <=720p keeps the spike fast; bv*+ba forces the ffmpeg merge
                // path, which is exactly what we need to validate.
                addOption("-f", "bv*[height<=720]+ba/b[height<=720]/b")
                addOption("--merge-output-format", "mp4")
                addOption("--restrict-filenames")
                addOption("--no-mtime")
            }
            val t0 = System.currentTimeMillis()
            YoutubeDL.getInstance().execute(request) { progress, etaSeconds, line ->
                runOnUiThread { progressBar.progress = progress.toInt().coerceIn(0, 100) }
                log("[%.1f%% eta %ds] %s".format(progress, etaSeconds, line.trim()))
            }
            log("download finished in ${(System.currentTimeMillis() - t0) / 1000}s")
            outDir.listFiles()?.forEach { f ->
                log("RESULT: ${f.name} (${f.length() / 1024 / 1024} MB)")
            }
            log("DOWNLOAD OK")
        } catch (e: Throwable) {
            log("DOWNLOAD FAILED: ${Log.getStackTraceString(e)}")
        }
    }

    // ==================== plumbing ====================

    private fun log(message: String) {
        Log.i("SPIKE", message)
        runOnUiThread {
            logView.append("$message\n")
            logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun buildUi() {
        val pad = (12 * resources.displayMetrics.density).toInt()
        urlInput = EditText(this).apply { hint = "Media URL" }
        infoButton = Button(this).apply { text = "Get info" }
        downloadButton = Button(this).apply { text = "Download" }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        logView = TextView(this).apply { textSize = 11f; setTextIsSelectable(true) }
        logScroll = ScrollView(this).apply {
            addView(logView)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0).apply { weight = 1f }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(infoButton, LinearLayout.LayoutParams(0, WRAP_CONTENT).apply { weight = 1f })
            addView(downloadButton, LinearLayout.LayoutParams(0, WRAP_CONTENT).apply { weight = 1f })
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            addView(urlInput)
            addView(buttons)
            addView(progressBar)
            addView(logScroll)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}
