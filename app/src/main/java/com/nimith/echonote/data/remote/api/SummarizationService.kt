package com.nimith.echonote.data.remote.api

import com.nimith.echonote.data.remote.models.SummarizationRequest
import com.nimith.echonote.data.remote.models.SummarizationResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SummarizationService {
    @POST("v1/responses")
    suspend fun summarize(
        @Header("Authorization") authorization: String,
        @Body request: SummarizationRequest
    ): SummarizationResponse
}
