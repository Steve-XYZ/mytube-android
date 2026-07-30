package com.mytube.android.data.download

import androidx.room.TypeConverter

class DownloadTypeConverters {
    @TypeConverter
    fun formatPresetToString(value: DownloadFormatPreset): String = value.name

    @TypeConverter
    fun stringToFormatPreset(value: String): DownloadFormatPreset =
        DownloadFormatPreset.valueOf(value)

    @TypeConverter
    fun stateToString(value: DownloadState): String = value.name

    @TypeConverter
    fun stringToState(value: String): DownloadState = DownloadState.valueOf(value)
}
