package com.nimith.echonote.domain.repository

import com.nimith.echonote.data.remote.models.SummarizationResponse

interface SummarizationRepository {

    suspend fun summarize(text: String): SummarizationResponse
}
