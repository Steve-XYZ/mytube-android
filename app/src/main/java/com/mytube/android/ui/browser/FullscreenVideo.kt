package com.mytube.android.ui.browser

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun FullscreenVideo(
    videoView: View,
    onExit: () -> Unit,
) {
    val hostView = LocalView.current

    BackHandler(onBack = onExit)

    DisposableEffect(hostView) {
        val activity = hostView.context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, hostView) }
        val previousOrientation = activity?.requestedOrientation

        insetsController?.apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        // Follow the sensor even when auto-rotate is locked so landscape video can fill the screen.
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        hostView.keepScreenOn = true

        onDispose {
            hostView.keepScreenOn = false
            previousOrientation?.let { activity.requestedOrientation = it }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    AndroidView(
        factory = {
            (videoView.parent as? ViewGroup)?.removeView(videoView)
            videoView
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    )
}
