package com.nimith.echonote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nimith.echonote.data.local.model.AudioChunk
import com.nimith.echonote.data.local.model.Converters
import com.nimith.echonote.data.local.model.Recording

@Database(entities = [Recording::class, AudioChunk::class], version = 2)
@TypeConverters(Converters::class)
abstract class EchoNoteDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao

    abstract fun audioChunkDao(): AudioChunkDao
}
