package com.nimith.echonote.data.local.model

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTranscriptionStatus(value: TranscriptionStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTranscriptionStatus(value: String): TranscriptionStatus {
        return TranscriptionStatus.valueOf(value)
    }

    @TypeConverter
    fun fromUploadStatus(value: UploadStatus): String {
        return value.name
    }

}
