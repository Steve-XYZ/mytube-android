package com.mytube.android.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mytube.android.MyTubeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CancelAction) return
        val id = intent.getStringExtra(DownloadIdExtra) ?: return
        val pendingResult = goAsync()
        val application = context.applicationContext as MyTubeApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                application.container.downloadCoordinator.cancel(id)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val CancelAction = "com.mytube.android.action.CANCEL_DOWNLOAD"
        const val DownloadIdExtra = "download_id"
    }
}
