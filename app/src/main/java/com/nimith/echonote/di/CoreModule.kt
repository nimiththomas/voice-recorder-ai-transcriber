package com.nimith.echonote.di

import android.content.Context
import android.media.AudioManager
import com.nimith.echonote.core.network.SafeApiCaller
import com.nimith.echonote.data.local.AudioChunkDao
import com.nimith.echonote.data.local.EchoNoteDatabase
import com.nimith.echonote.data.recorder.AudioRecorder
import com.nimith.echonote.domain.repository.RecordingRepository
import com.nimith.echonote.presentation.common.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideSafeApiCaller(): SafeApiCaller {
        return SafeApiCaller()
    }

    @Provides
    @Singleton
    fun provideAudioChunkDao(db: EchoNoteDatabase): AudioChunkDao = db.audioChunkDao()

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