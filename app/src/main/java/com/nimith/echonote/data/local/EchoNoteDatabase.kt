package com.nimith.echonote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Transcript::class], version = 1)
abstract class EchoNoteDatabase : RoomDatabase() {
}