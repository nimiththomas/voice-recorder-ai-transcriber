package com.nimith.echonote.data.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.nimith.echonote.presentation.common.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class AudioRecorder(private val context: Context) {

    @Volatile
    private var recorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)


    fun start() {
        if (recordingJob?.isActive == true) {
            Log.i(LOG_TAG, "Recording already in progress")
            return
        }
        recordingJob = scope.launch {
            while (true) {
                launch { recordChunk() }
                delay(CHUNK_DURATION_MS - CHUNK_OVERLAP_MS)
            }
        }
    }

    private suspend fun recordChunk() {
        val newRecorder = createRecorder()
        try {
            newRecorder.prepare()
            newRecorder.start()
            recorder = newRecorder
            delay(CHUNK_DURATION_MS)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Prepare failed for new recording chunk", e)
        } catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "Start failed for new recording chunk", e)
        } catch (e: CancellationException) {
            // Coroutine was cancelled, this is expected on stop().
            throw e // Re-throw CancellationException to be handled by the coroutine framework
        } catch (e: Exception) {
            Log.e(LOG_TAG, "An unexpected error occurred during recording chunk", e)
        } finally {
            try {
                // It's possible for stop() to throw an exception if the recording was never started
                newRecorder.stop()
            } catch (e: RuntimeException) {
                // Ignore if stop() is called after an error or if not started
                Log.w(LOG_TAG, "Stop failed for recording chunk", e)
            }
            newRecorder.release()
            if (recorder == newRecorder) {
                recorder = null
            }
        }
    }

    private fun createRecorder(): MediaRecorder {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileUtils.getRecordingFilePath(context))
        }
    }

    fun stop() {
        if (recordingJob?.isActive != true) {
            return
        }
        recordingJob?.cancel()
        recordingJob = null
        recorder = null // Ensure recorder is cleared
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_: IllegalStateException) {
            // This can happen if getMaxAmplitude is called after the recorder has been released.
            0
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error retrieving max amplitude", e)
            0
        }
    }

    companion object {
        private const val LOG_TAG = "AudioRecorder"
        private const val CHUNK_DURATION_MS = 30_000L
        private const val CHUNK_OVERLAP_MS = 2_000L
    }
}
