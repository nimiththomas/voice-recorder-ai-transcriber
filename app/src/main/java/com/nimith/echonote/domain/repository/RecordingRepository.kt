package com.nimith.echonote.domain.repository

import com.nimith.echonote.data.local.model.AudioChunk
import com.nimith.echonote.data.local.model.Recording

interface RecordingRepository {
    suspend fun insertRecording(recording: Recording): Long
    suspend fun getRecording(id: Long): Recording?
    suspend fun updateRecording(recording: Recording)
    suspend fun getLastChunkIndex(recordingId: Long): Int?
    suspend fun insertChunk(chunk: AudioChunk): Long
}