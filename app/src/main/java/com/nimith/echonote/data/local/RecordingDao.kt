package com.nimith.echonote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nimith.echonote.data.local.model.Recording

@Dao
interface RecordingDao {
    @Insert
    suspend fun insertRecording(recording: Recording): Long

    @Update
    suspend fun updateRecording(recording: Recording)

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecording(id: Long): Recording?
}
