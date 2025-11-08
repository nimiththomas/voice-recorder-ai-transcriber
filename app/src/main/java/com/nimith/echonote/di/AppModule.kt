package com.nimith.echonote.di

import android.content.Context
import android.media.AudioManager
import androidx.room.Room
import com.nimith.echonote.data.local.AudioChunkDao
import com.nimith.echonote.data.local.EchoNoteDatabase
import com.nimith.echonote.data.local.RecordingDao
import com.nimith.echonote.data.recorder.AudioRecorder
import com.nimith.echonote.data.repository.RecordingRepository
import com.nimith.echonote.data.repository.RecordingRepositoryImpl
import com.nimith.echonote.presentation.common.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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

    @Provides
    @Singleton
    fun provideAudioChunkDao(db: EchoNoteDatabase): AudioChunkDao = db.audioChunkDao()

    @Provides
    @Singleton
    fun provideRecordingRepository(recordingDao: RecordingDao, audioChunkDao: AudioChunkDao): RecordingRepository {
        return RecordingRepositoryImpl(recordingDao, audioChunkDao)
    }

    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(
        @ApplicationContext context: Context,
        recordingRepository: RecordingRepository
    ): AudioRecorder {
        return AudioRecorder(context, recordingRepository)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }
}
