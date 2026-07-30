package com.mytube.android

import android.app.Application

class MyTubeApplication : Application() {
    val container by lazy { AppContainer(this) }
}
