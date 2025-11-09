package com.nimith.echonote.data.repository

import com.nimith.echonote.BuildConfig
import com.nimith.echonote.data.remote.api.SummarizationService
import com.nimith.echonote.data.remote.models.SummarizationRequest
import com.nimith.echonote.data.remote.models.SummarizationResponse
import com.nimith.echonote.domain.repository.SummarizationRepository
import javax.inject.Inject

class SummarizationRepositoryImpl @Inject constructor(
    private val summarizationService: SummarizationService
) : SummarizationRepository {

    override suspend fun summarize(text: String): SummarizationResponse {
        val request = SummarizationRequest(
            model = "gpt-4.1",
            input = text
        )
        return summarizationService.summarize(BuildConfig.OPENAI_API_KEY, request)
    }
}
