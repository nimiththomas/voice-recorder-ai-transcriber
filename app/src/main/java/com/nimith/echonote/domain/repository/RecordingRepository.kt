package com.nimith.echonote.domain.repository

import com.nimith.echonote.data.local.model.AudioChunk
import com.nimith.echonote.data.local.model.Recording
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    suspend fun insertRecording(recording: Recording): Long
    fun getRecording(id: Long): Flow<Recording?>
    suspend fun updateRecording(recording: Recording)
    suspend fun updateRecordingSummary(recordingId: Long, summary: String?)
    suspend fun getLastChunkIndex(recordingId: Long): Int?
    suspend fun insertChunk(chunk: AudioChunk): Long
    fun getLatestRecording(): Flow<Recording?>
    fun getChunksForRecording(recordingId: Long): Flow<List<AudioChunk>>
    suspend fun getChunk(recordingId: Long, chunkIndex: Int): AudioChunk?
    suspend fun updateChunk(chunk: AudioChunk)
    fun getCompletedChunksForRecording(recordingId: Long): Flow<List<AudioChunk>>
}