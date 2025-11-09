package com.nimith.echonote.data.local

import androidx.room.Dao
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

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecording(id: Long): Flow<Recording?>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC LIMIT 1")
    fun getLatestRecording(): Flow<Recording?>
}
