package com.nimith.echonote.data.repository

import com.nimith.echonote.data.local.AudioChunkDao
import com.nimith.echonote.data.local.RecordingDao
import com.nimith.echonote.data.local.model.AudioChunk
import com.nimith.echonote.data.local.model.Recording
import com.nimith.echonote.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val audioChunkDao: AudioChunkDao
) : RecordingRepository {
    override suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insertRecording(recording)
    }

    override fun getRecording(id: Long): Flow<Recording?> {
        return recordingDao.getRecording(id)
    }

    override suspend fun updateRecording(recording: Recording) {
        recordingDao.updateRecording(recording)
    }

    override suspend fun updateRecordingSummary(recordingId: Long, summary: String?) {
        recordingDao.updateSummary(recordingId, summary)
    }

    override suspend fun getLastChunkIndex(recordingId: Long): Int? {
        return audioChunkDao.getLastChunkIndex(recordingId)
    }

    override suspend fun insertChunk(chunk: AudioChunk): Long {
        return audioChunkDao.insertChunk(chunk)
    }

    override fun getLatestRecording(): Flow<Recording?> {
        return recordingDao.getLatestRecording()
    }

    override fun getChunksForRecording(recordingId: Long): Flow<List<AudioChunk>> {
        return audioChunkDao.getChunksForRecording(recordingId)
    }

    override suspend fun getChunk(recordingId: Long, chunkIndex: Int): AudioChunk? {
        return audioChunkDao.getChunk(recordingId, chunkIndex)
    }

    override suspend fun updateChunk(chunk: AudioChunk) {
        audioChunkDao.updateChunk(chunk)
    }

    override fun getCompletedChunksForRecording(recordingId: Long): Flow<List<AudioChunk>> {
        return audioChunkDao.getCompletedChunksForRecording(recordingId)
    }

    override fun getRecordingsWithSummary(): Flow<List<Recording>> {
        return recordingDao.getRecordingsWithSummary()
    }

    override suspend fun deleteRecording(recording: Recording) {
        recordingDao.deleteRecording(recording)
    }
}
