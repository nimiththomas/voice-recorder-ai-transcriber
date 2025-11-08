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
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nimith.echonote.data.local.model.Recording
import com.nimith.echonote.data.local.model.TranscriptionStatus
import com.nimith.echonote.data.recorder.AudioRecorder
import com.nimith.echonote.data.repository.RecordingRepository
import com.nimith.echonote.presentation.common.Constants.ACTION_PAUSE
import com.nimith.echonote.presentation.common.Constants.ACTION_RESUME
import com.nimith.echonote.presentation.common.Constants.ACTION_START
import com.nimith.echonote.presentation.common.Constants.ACTION_STOP
import com.nimith.echonote.presentation.common.Constants.NOTIFICATION_ID
import com.nimith.echonote.presentation.common.NotificationHelper
import com.nimith.echonote.presentation.common.StorageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        if (isRecording) return

        if (!StorageUtils.isStorageAvailable(this)) {
            Toast.makeText(this, "Recording stopped - Low storage", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        if (requestAudioFocus()) {
            lifecycleScope.launch {
                val newRecording = Recording(
                    title = "Recording ${System.currentTimeMillis()}",
                    createdAt = System.currentTimeMillis(),
                    duration = 0,
                    transcriptionStatus = TranscriptionStatus.IN_PROGRESS
                )
                currentRecordingId = recordingRepository.insertRecording(newRecording)

                isRecording = true
                recordingStartTime = SystemClock.elapsedRealtime()
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
        isRecording = false

        lifecycleScope.launch {
            audioRecorder.stop()
            currentRecordingId?.let {
                val recording = recordingRepository.getRecording(it)
                recording?.let { rec ->
                    val duration = SystemClock.elapsedRealtime() - recordingStartTime
                    val updatedRecording = rec.copy(
                        duration = duration,
                        transcriptionStatus = TranscriptionStatus.COMPLETED
                    )
                    recordingRepository.updateRecording(updatedRecording)
                }
            }
        }

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
            if (isPausedByCall) updateNotification("Paused - Phone call")
            else updateNotification("Paused - Audio focus lost", addResumeAction = true)
            return
        }

        if (!requestAudioFocus()) {
            Toast.makeText(this, "Could not regain audio focus.", Toast.LENGTH_SHORT).show()
            stop()
            return
        }

        lifecycleScope.launch {
            currentRecordingId?.let {
                val lastChunkIndex = recordingRepository.getLastChunkIndex(it)
                audioRecorder.start(it, lastChunkIndex)
                recordingStartTime = SystemClock.elapsedRealtime() - timeWhenPaused
                startSilenceDetection()
                startProgressUpdates()
                updateNotification("Recording")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            try {
                notificationHelper.notificationManager.notify(
                    NOTIFICATION_ID,
                    notification
                )
            } catch (_: Exception) {
                notificationHelper.notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } else {
            notificationHelper.notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = lifecycleScope.launch {
            while (isRecording && !isPausedByCall && !isPausedByFocusLoss) {
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
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (isRecording) pauseRecording(isCall = false)
            AudioManager.AUDIOFOCUS_GAIN -> if (isPausedByFocusLoss) resumeRecording(isCall = false)
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN)
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

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stop()
        stopProgressUpdates()
        unregisterReceiver(phoneStateReceiver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }
}

