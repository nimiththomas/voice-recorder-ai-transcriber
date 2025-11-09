package com.nimith.echonote.core.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nimith.echonote.core.recorder.AudioRecorder
import com.nimith.echonote.domain.repository.RecordingRepository
import com.nimith.echonote.domain.repository.SummarizationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SummarizationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val summarizationRepository: SummarizationRepository,
    private val recordingRepository: RecordingRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount > 10) { // Stop retrying after 10 attempts
            return Result.failure()
        }

        val recordingId = inputData.getLong("recordingId", -1)
        if (recordingId == -1L) {
            return Result.failure() // Don't retry for invalid input
        }

        val workManager = WorkManager.getInstance(appContext)
        val transcriptionWorkTag = "${AudioRecorder.TRANSCRIPTION_WORK_TAG_PREFIX}$recordingId"

        try {
            val transcriptionWorkInfos = workManager.getWorkInfosByTag(transcriptionWorkTag).get()

            // If any transcription worker is still running/enqueued, we need to wait.
            val isTranscriptionOngoing = transcriptionWorkInfos.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            }
            if (isTranscriptionOngoing) {
                return Result.retry()
            }

            val chunks = recordingRepository.getChunksForRecording(recordingId).first()

            // We only care about chunks that have a non-blank transcription.
            val fullTranscript = chunks
                .filter { !it.transcription.isNullOrBlank() }
                .sortedBy { it.chunkIndex }
                .joinToString(" ") { it.transcription!! }
                .trim()

            if (fullTranscript.isBlank()) {
                // This is a failure because there's nothing to summarize.
                // We don't retry, as all workers have settled.
                return Result.failure()
            }

            val summary = summarizationRepository.summarize(fullTranscript).getSummaryText()
            recordingRepository.updateRecordingSummary(recordingId, summary)

            return Result.success()
        } catch (e: Exception) {
            Log.e("SummarizationWorker", "An error occurred during summarization", e)
            return Result.retry()
        }
    }
}
