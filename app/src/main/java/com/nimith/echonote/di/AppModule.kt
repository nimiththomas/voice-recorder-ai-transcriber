package com.nimith.echonote.di

import com.nimith.echonote.domain.repository.RecordingRepository
import com.nimith.echonote.data.repository.RecordingRepositoryImpl
import com.nimith.echonote.data.repository.TranscriptionRepositoryImpl
import com.nimith.echonote.domain.repository.TranscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(
        transcriptionRepositoryImpl: TranscriptionRepositoryImpl
    ): TranscriptionRepository

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        recordingRepositoryImpl: RecordingRepositoryImpl
    ): RecordingRepository
}
