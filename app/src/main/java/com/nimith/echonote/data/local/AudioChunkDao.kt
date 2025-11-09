package com.nimith.echonote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nimith.echonote.data.local.model.AudioChunk
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {
    @Insert
    suspend fun insertChunk(chunk: AudioChunk): Long

    @Query("SELECT MAX(chunkIndex) FROM audio_chunks WHERE recordingId = :recordingId")
    suspend fun getLastChunkIndex(recordingId: Long): Int?

    @Query("SELECT * FROM audio_chunks WHERE recordingId = :recordingId ORDER BY chunkIndex")
    fun getChunksForRecording(recordingId: Long): Flow<List<AudioChunk>>

    @Query("SELECT * FROM audio_chunks WHERE recordingId = :recordingId AND chunkIndex = :chunkIndex")
    suspend fun getChunk(recordingId: Long, chunkIndex: Int): AudioChunk?

    @Update
    suspend fun updateChunk(chunk: AudioChunk)

    @Query("SELECT * FROM audio_chunks WHERE recordingId = :recordingId AND uploadStatus = 'COMPLETED' ORDER BY chunkIndex")
    fun getCompletedChunksForRecording(recordingId: Long): Flow<List<AudioChunk>>
}
