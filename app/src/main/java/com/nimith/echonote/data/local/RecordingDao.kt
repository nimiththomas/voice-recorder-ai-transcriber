package com.nimith.echonote.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nimith.echonote.data.local.model.Recording
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    suspend fun insertRecording(recording: Recording): Long

    @Update
    suspend fun updateRecording(recording: Recording)

    @Query("UPDATE recordings SET summary = :summary WHERE id = :recordingId")
    suspend fun updateSummary(recordingId: Long, summary: String?)

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecording(id: Long): Flow<Recording?>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC LIMIT 1")
    fun getLatestRecording(): Flow<Recording?>

    @Query("SELECT * FROM recordings WHERE summary IS NOT NULL ORDER BY createdAt DESC")
    fun getRecordingsWithSummary(): Flow<List<Recording>>

    @Delete
    suspend fun deleteRecording(recording: Recording)
}
