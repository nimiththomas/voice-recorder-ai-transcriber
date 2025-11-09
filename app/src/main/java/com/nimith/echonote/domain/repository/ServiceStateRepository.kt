package com.nimith.echonote.domain.repository

import com.nimith.echonote.data.local.model.ServiceState
import kotlinx.coroutines.flow.Flow

interface ServiceStateRepository {
    val serviceState: Flow<ServiceState?>
    suspend fun saveServiceState(serviceState: ServiceState)
    suspend fun clearServiceState()
}
