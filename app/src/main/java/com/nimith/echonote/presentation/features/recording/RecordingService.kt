package com.nimith.echonote.presentation.features.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nimith.echonote.core.recorder.AudioRecorder
import com.nimith.echonote.data.local.model.Recording
import com.nimith.echonote.data.local.model.ServiceState
import com.nimith.echonote.data.local.model.TranscriptionStatus
import com.nimith.echonote.domain.repository.RecordingRepository
import com.nimith.echonote.domain.repository.ServiceStateRepository
import com.nimith.echonote.presentation.common.Constants.ACTION_PAUSE
import com.nimith.echonote.presentation.common.Constants.ACTION_RESUME
import com.nimith.echonote.presentation.common.Constants.ACTION_START
import com.nimith.echonote.presentation.common.Constants.ACTION_STOP
import com.nimith.echonote.presentation.common.Constants.NOTIFICATION_ID
import com.nimith.echonote.presentation.common.NotificationHelper
import com.nimith.echonote.presentation.common.StorageUtils
import com.nimith.echonote.workers.SummarizationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : LifecycleService(), AudioManager.OnAudioFocusChangeListener {

    private var isRecording = false
    private var isPausedByCall = false
    private var isPausedByFocusLoss = false

    @Inject
    lateinit var audioManager: AudioManager

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var recordingRepository: RecordingRepository

    @Inject
    lateinit var serviceStateRepository: ServiceStateRepository

    @Inject
    lateinit var stateHolder: RecordingStateHolder

    private var audioFocusRequest: AudioFocusRequest? = null
    private var silenceDetectionJob: Job? = null
    private var progressUpdateJob: Job? = null

    private var recordingStartTime: Long = 0
    private var timeWhenPaused: Long = 0
    private var lastAmplitude: Int = 0
    private var silenceStartTime: Long = 0
    private var currentRecordingId: Long? = null

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                    TelephonyManager.EXTRA_STATE_RINGING,
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (isRecording) pauseRecording(isCall = true)
                    }

                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (isRecording) resumeRecording(isCall = true)
                    }
                }
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            handleAudioDeviceChange()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            handleAudioDeviceChange()
        }
    }

    private fun handleAudioDeviceChange() {
        lifecycleScope.launch {
            delay(300)
            Toast.makeText(this@RecordingService, "Audio source changed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannel()

        val initialNotification =
            notificationHelper.createNotification("EchoNote is ready to record")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        val phoneStateFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(phoneStateReceiver, phoneStateFilter)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) {
            handleRestart()
        } else {
            when (intent.action) {
                ACTION_START -> start()
                ACTION_PAUSE -> pauseRecording()
                ACTION_RESUME -> resumeRecording()
                ACTION_STOP -> stop()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(phoneStateReceiver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    private fun start() {
        lifecycleScope.launch {
            if (isRecording) return@launch

            val previousState = serviceStateRepository.serviceState.first()
            if (previousState?.isRecording == true) {
                handleRestart()
                return@launch
            }

            if (!StorageUtils.isStorageAvailable(this@RecordingService)) {
                Toast.makeText(this@RecordingService, "Recording stopped - Low storage", Toast.LENGTH_LONG).show()
                stopSelf()
                return@launch
            }

            if (requestAudioFocus()) {
                val newRecording = Recording(
                    title = "Recording ${System.currentTimeMillis()}",
                    createdAt = System.currentTimeMillis(),
                    duration = 0,
                    transcriptionStatus = TranscriptionStatus.IN_PROGRESS
                )
                currentRecordingId = recordingRepository.insertRecording(newRecording)

                isRecording = true
                recordingStartTime = SystemClock.elapsedRealtime()
                serviceStateRepository.saveServiceState(
                    ServiceState(
                        isRecording = true,
                        recordingId = currentRecordingId!!,
                        startTimeElapsed = recordingStartTime
                    )
                )

                stateHolder.update { it.copy(isRecording = true, timerMillis = 0L, recordingId = currentRecordingId) }
                audioRecorder.start(currentRecordingId!!)
                startSilenceDetection()
                startProgressUpdates()
                isPausedByCall = false
                isPausedByFocusLoss = false
                updateNotification("Recording")
            }
        }
    }

    private fun stop() {
        if (!isRecording) return

        lifecycleScope.launch {
            serviceStateRepository.clearServiceState()
        }

        isRecording = false

        lifecycleScope.launch {
            audioRecorder.stop()
            currentRecordingId?.let {
                val recording = recordingRepository.getRecording(it).firstOrNull()
                recording?.let { rec ->
                    val duration = SystemClock.elapsedRealtime() - recordingStartTime
                    val updatedRecording = rec.copy(
                        duration = duration,
                    )
                    recordingRepository.updateRecording(updatedRecording)
                }
            }
        }
        currentRecordingId?.let {
            val summarizationWorkRequest = OneTimeWorkRequestBuilder<SummarizationWorker>()
                .setInputData(workDataOf("recordingId" to it))
                .build()
            WorkManager.getInstance(applicationContext).enqueue(summarizationWorkRequest)
            Log.d("RecordingService", "SummarizationWorker enqueued for recordingId: $it")
        }

        stateHolder.update { it.copy(isRecording = false, recordingId = null) }

        releaseAudioFocus()
        stopSilenceDetection()
        stopProgressUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleRecordingFailure() {
        if (!isRecording && currentRecordingId == null) return

        isRecording = false

        lifecycleScope.launch {
            audioRecorder.stop()
            serviceStateRepository.clearServiceState()
            currentRecordingId?.let {
                val recording = recordingRepository.getRecording(it).firstOrNull()
                recording?.let { rec ->
                    val updatedRecording = rec.copy(
                        transcriptionStatus = TranscriptionStatus.FAILED
                    )
                    recordingRepository.updateRecording(updatedRecording)
                }
            }
        }
        stateHolder.update { it.copy(isRecording = false, recordingId = null) }

        releaseAudioFocus()
        stopSilenceDetection()
        stopProgressUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseRecording(isCall: Boolean = false) {
        if (!isRecording) return

        val wasPaused = isPausedByCall || isPausedByFocusLoss
        if (isCall) isPausedByCall = true else isPausedByFocusLoss = true

        if (wasPaused) {
            if (isPausedByCall) updateNotification("Paused - Phone call")
            else updateNotification("Paused - Audio focus lost", addResumeAction = true)
            return
        }

        audioRecorder.stop()
        timeWhenPaused = SystemClock.elapsedRealtime() - recordingStartTime
        stopSilenceDetection()
        stopProgressUpdates()
        stateHolder.update { it.copy(isRecording = true, isPaused = true) }

        if (isCall) updateNotification("Paused - Phone call")
        else updateNotification("Paused - Audio focus lost", addResumeAction = true)
    }

    private fun resumeRecording(isCall: Boolean = false) {
        if (!isRecording) return

        if (isCall) {
            if (!isPausedByCall) return
            isPausedByCall = false
        } else {
            if (!isPausedByFocusLoss) return
            isPausedByFocusLoss = false
        }

        if (isPausedByCall || isPausedByFocusLoss) {
            if (isPausedByCall) updateNotification(
                "Paused - Phone call")
            else updateNotification("Paused - Audio focus lost", addResumeAction = true)
            return
        }

        if (!requestAudioFocus()) {
            Toast.makeText(this, "Could not regain audio focus.", Toast.LENGTH_SHORT).show()
            handleRecordingFailure()
            return
        }

        lifecycleScope.launch {
            currentRecordingId?.let { recordingId ->
                val lastChunkIndex = recordingRepository.getLastChunkIndex(recordingId)
                audioRecorder.start(recordingId, lastChunkIndex)
                recordingStartTime = SystemClock.elapsedRealtime() - timeWhenPaused
                stateHolder.update { it.copy(isRecording = true, isPaused = false) }
                startSilenceDetection()
                startProgressUpdates()
                updateNotification("Recording")
            }
        }
    }

    private fun handleRestart() {
        lifecycleScope.launch {
            val serviceState = serviceStateRepository.serviceState.first()
            if (serviceState?.isRecording == true) {
                currentRecordingId = serviceState.recordingId
                recordingStartTime = serviceState.startTimeElapsed
                isRecording = true

                stateHolder.update {
                    it.copy(
                        isRecording = true,
                        timerMillis = SystemClock.elapsedRealtime() - recordingStartTime,
                        recordingId = currentRecordingId
                    )
                }

                if (requestAudioFocus()) {
                    val lastChunkIndex = recordingRepository.getLastChunkIndex(currentRecordingId!!)
                    audioRecorder.start(currentRecordingId!!, lastChunkIndex)
                    startSilenceDetection()
                    startProgressUpdates()
                    updateNotification("Recording")
                } else {
                    Toast.makeText(this@RecordingService, "Could not regain audio focus on restart.", Toast.LENGTH_SHORT).show()
                    handleRecordingFailure()
                }
            }
        }
    }

    private fun updateNotification(contentText: String, addResumeAction: Boolean = false) {
        val notification = notificationHelper.buildNotificationForUpdate(
            contentText,
            addResumeAction,
            isRecording,
            isPausedByCall,
            isPausedByFocusLoss,
            recordingStartTime
        )
        notificationHelper.postNotification(NOTIFICATION_ID, notification)
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = lifecycleScope.launch { 
            while (isRecording && !isPausedByCall && !isPausedByFocusLoss) {
                val elapsedMillis = SystemClock.elapsedRealtime() - recordingStartTime
                stateHolder.update { it.copy(timerMillis = elapsedMillis) }
                updateNotification("Recording in progress...")
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun startSilenceDetection() {
        silenceDetectionJob?.cancel()
        silenceDetectionJob = lifecycleScope.launch {
            while (isRecording) {
                val currentAmplitude = audioRecorder.getMaxAmplitude()
                if (currentAmplitude == lastAmplitude && currentAmplitude < 100) {
                    if (silenceStartTime == 0L) {
                        silenceStartTime = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - silenceStartTime >= 10000) {
                        updateNotification("No audio detected - Check microphone")
                    }
                } else {
                    silenceStartTime = 0L
                }
                lastAmplitude = currentAmplitude
                delay(2000)
            }
        }
    }

    private fun stopSilenceDetection() {
        silenceDetectionJob?.cancel()
        silenceStartTime = 0L
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> if (isRecording) pauseRecording(isCall = false)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (isRecording) pauseRecording(
                isCall = false
            )

            AudioManager.AUDIOFOCUS_GAIN -> if (isPausedByFocusLoss) resumeRecording(isCall = false)
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }
}
