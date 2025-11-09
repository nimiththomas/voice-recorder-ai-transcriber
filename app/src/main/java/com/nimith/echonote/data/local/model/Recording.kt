package com.nimith.echonote.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TranscriptionStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val duration: Long,
    val transcriptionStatus: TranscriptionStatus,
    val transcription: String? = null
)
