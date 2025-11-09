package com.nimith.echonote.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val preferencesFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    fun <T> readValue(key: Preferences.Key<T>): Flow<T?> {
        return preferencesFlow.map { preferences ->
            preferences[key]
        }
    }

    fun <T> readValue(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return preferencesFlow.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.clear()
        }
    }
}