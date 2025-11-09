package com.nimith.echonote.data.repository

import com.nimith.echonote.BuildConfig
import com.nimith.echonote.core.network.NetworkResult
import com.nimith.echonote.core.network.SafeApiCaller
import com.nimith.echonote.data.remote.api.TranscriptionService
import com.nimith.echonote.data.remote.models.TranscriptionResponse
import com.nimith.echonote.domain.repository.TranscriptionRepository
import com.nimith.echonote.presentation.common.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class TranscriptionRepositoryImpl @Inject constructor(
    private val transcriptionService: TranscriptionService,
    private val safeApiCaller: SafeApiCaller
) : TranscriptionRepository {
    override suspend fun transcribe(file: File): NetworkResult<TranscriptionResponse> {

        val requestBody = file.asRequestBody(Constants.MEDIA_TYPE_AUDIO.toMediaTypeOrNull())
        val multipartBody = MultipartBody.Part.createFormData(Constants.PART_NAME_FILE, file.name, requestBody)

        return safeApiCaller.safeApiCall {
            transcriptionService.transcribe(
                "${Constants.AUTH_BEARER}${BuildConfig.OPENAI_API_KEY}",
                multipartBody,
                Constants.TRANSCRIPTION_MODEL.toRequestBody(Constants.MEDIA_TYPE_TEXT_PLAIN.toMediaType())
            )
        }
    }

}
