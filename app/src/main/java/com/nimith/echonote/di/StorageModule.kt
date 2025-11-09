package com.nimith.echonote.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.nimith.echonote.data.local.EchoNoteDatabase
import com.nimith.echonote.data.local.RecordingDao
import com.nimith.echonote.data.local.datastore.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton



@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

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


    private const val ECHONOTE_PREFERENCES = "echonote_preferences"

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(ECHONOTE_PREFERENCES) }
        )
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(dataStore: DataStore<Preferences>): DataStoreManager {
        return DataStoreManager(dataStore)
    }
}