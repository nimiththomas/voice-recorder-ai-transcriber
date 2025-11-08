package com.nimith.echonote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nimith.echonote.data.local.model.AudioChunk

@Dao
interface AudioChunkDao {
    @Insert
    suspend fun insertChunk(chunk: AudioChunk): Long

    @Query("SELECT MAX(chunkIndex) FROM audio_chunks WHERE recordingId = :recordingId")
    suspend fun getLastChunkIndex(recordingId: Long): Int?
}
