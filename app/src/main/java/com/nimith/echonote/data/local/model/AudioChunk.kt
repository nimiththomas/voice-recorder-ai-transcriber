package com.nimith.echonote.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = Recording::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordingId"])]
)
data class AudioChunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordingId: Long,
    val chunkIndex: Int,
    val filePath: String,
    var transcription: String?,
    var uploadStatus: UploadStatus,
    val createdAt: Long
)
