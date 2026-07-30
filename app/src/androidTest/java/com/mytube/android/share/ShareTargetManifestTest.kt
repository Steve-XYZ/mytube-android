package com.mytube.android.share

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mytube.android.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareTargetManifestTest {

    @Test
    fun appIsRegisteredAsPlainTextShareTarget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .setPackage(context.packageName)
            .putExtra(Intent.EXTRA_TEXT, "https://youtu.be/aqz-KE-bpKQ")

        val activities = context.packageManager.queryIntentActivities(
            intent,
            0,
        )

        assertTrue(
            activities.any {
                it.activityInfo.name == MainActivity::class.java.name
            },
        )
    }
}
