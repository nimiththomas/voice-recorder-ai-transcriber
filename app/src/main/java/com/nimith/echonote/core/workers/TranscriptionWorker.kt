package com.nimith.echonote.core.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nimith.echonote.core.network.NetworkResult
import com.nimith.echonote.data.local.model.UploadStatus
import com.nimith.echonote.data.remote.models.TranscriptionResponse
import com.nimith.echonote.domain.repository.RecordingRepository
import com.nimith.echonote.domain.repository.TranscriptionRepository
import com.nimith.echonote.presentation.common.Constants
import com.nimith.echonote.presentation.common.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionRepository: TranscriptionRepository,
    private val recordingRepository: RecordingRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong("recordingId", -1)
        val chunkIndex = inputData.getInt("chunkIndex", -1)
        if (recordingId == -1L || chunkIndex == -1) {
            return Result.failure()
        }

        val audioChunk = recordingRepository.getChunk(recordingId, chunkIndex)
            ?: return Result.failure()

        if (audioChunk.uploadStatus == UploadStatus.COMPLETED) {
            return Result.success()
        }

        notificationHelper.createNotificationChannel()
        val notification = notificationHelper.createTranscriptionNotification()
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                Constants.TRANSCRIPTION_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(Constants.TRANSCRIPTION_NOTIFICATION_ID, notification)
        }
        setForeground(foregroundInfo)

        audioChunk.uploadStatus = UploadStatus.UPLOADING
        recordingRepository.updateChunk(audioChunk)

        val file = File(audioChunk.filePath)

        return when (val result = transcriptionRepository.transcribe(file)) {

            is NetworkResult.Error -> {
                audioChunk.uploadStatus = UploadStatus.FAILED
                recordingRepository.updateChunk(audioChunk)
                Result.retry()
            }

            is NetworkResult.Success<TranscriptionResponse> -> {
                audioChunk.transcription = result.data.text
                audioChunk.uploadStatus = UploadStatus.COMPLETED
                recordingRepository.updateChunk(audioChunk)
                Result.success()
            }
        }
    }
}