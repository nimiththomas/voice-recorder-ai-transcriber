package com.nimith.echonote.di

import android.content.Context
import androidx.room.Room
import com.nimith.echonote.data.local.EchoNoteDatabase
import com.nimith.echonote.data.local.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEchoNoteDatabase(@ApplicationContext context: Context): EchoNoteDatabase {
        return Room.databaseBuilder(
            context,
            EchoNoteDatabase::class.java,
            "echonote_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRecordingDao(db: EchoNoteDatabase): RecordingDao = db.recordingDao()


}
