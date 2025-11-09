package com.nimith.echonote.di

import com.nimith.echonote.data.remote.api.TranscriptionService
import com.nimith.echonote.BuildConfig
import com.nimith.echonote.data.remote.api.MockTranscriptionService
import com.nimith.echonote.presentation.common.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAiService(retrofit: Retrofit): TranscriptionService {
        return if (BuildConfig.OPENAI_API_KEY.isEmpty()) {
            MockTranscriptionService()
        } else {
            retrofit.create(TranscriptionService::class.java)
        }
    }
}
