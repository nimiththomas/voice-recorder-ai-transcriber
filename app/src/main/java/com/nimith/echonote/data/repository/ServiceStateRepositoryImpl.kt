package com.nimith.echonote.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.nimith.echonote.data.local.datastore.DataStoreManager
import com.nimith.echonote.data.local.model.ServiceState
import com.nimith.echonote.domain.repository.ServiceStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ServiceStateRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ServiceStateRepository {

    private object PreferencesKeys {
        val IS_RECORDING = booleanPreferencesKey("is_recording")
        val RECORDING_ID = longPreferencesKey("recording_id")
        val START_TIME_ELAPSED = longPreferencesKey("start_time_elapsed")
    }

    override val serviceState: Flow<ServiceState?> = dataStoreManager.preferencesFlow
        .map { preferences ->
            val isRecording = preferences[PreferencesKeys.IS_RECORDING]
            val recordingId = preferences[PreferencesKeys.RECORDING_ID]
            val startTimeElapsed = preferences[PreferencesKeys.START_TIME_ELAPSED]

            if (isRecording != null && recordingId != null && startTimeElapsed != null) {
                ServiceState(isRecording, recordingId, startTimeElapsed)
            } else {
                null
            }
        }

    override suspend fun saveServiceState(serviceState: ServiceState) {
        dataStoreManager.setValue(PreferencesKeys.IS_RECORDING, serviceState.isRecording)
        dataStoreManager.setValue(PreferencesKeys.RECORDING_ID, serviceState.recordingId)
        dataStoreManager.setValue(PreferencesKeys.START_TIME_ELAPSED, serviceState.startTimeElapsed)
    }

    override suspend fun clearServiceState() {
        dataStoreManager.clear()
    }
}