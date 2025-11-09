package com.nimith.echonote.domain.repository

import com.nimith.echonote.core.network.NetworkResult
import com.nimith.echonote.data.remote.models.TranscriptionResponse
import java.io.File

interface TranscriptionRepository {
    suspend fun transcribe(file: File): NetworkResult<TranscriptionResponse>
}