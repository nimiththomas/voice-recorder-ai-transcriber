package com.nimith.echonote.data.repository

import com.nimith.echonote.data.local.AudioChunkDao
import com.nimith.echonote.data.local.RecordingDao
import com.nimith.echonote.data.local.model.AudioChunk
import com.nimith.echonote.data.local.model.Recording
import com.nimith.echonote.domain.repository.RecordingRepository
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val audioChunkDao: AudioChunkDao
) : RecordingRepository {
    override suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insertRecording(recording)
    }

    override suspend fun getRecording(id: Long): Recording? {
        return recordingDao.getRecording(id)
    }

    override suspend fun updateRecording(recording: Recording) {
        recordingDao.updateRecording(recording)
    }

    override suspend fun getLastChunkIndex(recordingId: Long): Int? {
        return audioChunkDao.getLastChunkIndex(recordingId)
    }

    override suspend fun insertChunk(chunk: AudioChunk): Long {
        return audioChunkDao.insertChunk(chunk)
    }
}
