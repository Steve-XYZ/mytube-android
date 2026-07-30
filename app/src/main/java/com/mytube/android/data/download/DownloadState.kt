package com.mytube.android.data.download

enum class DownloadState {
    Queued,
    Running,
    Paused,
    Completed,
    Failed,
    Cancelled,
}

val DownloadState.isTerminal: Boolean
    get() = this == DownloadState.Completed ||
        this == DownloadState.Failed ||
        this == DownloadState.Cancelled
