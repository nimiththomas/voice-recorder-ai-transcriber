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
import com.nimith.echonote.data.recorder.AudioRecorder
import com.nimith.echonote.presentation.common.Constants.ACTION_PAUSE
import com.nimith.echonote.presentation.common.Constants.ACTION_RESUME
import com.nimith.echonote.presentation.common.Constants.ACTION_START
import com.nimith.echonote.presentation.common.Constants.ACTION_STOP
import com.nimith.echonote.presentation.common.Constants.NOTIFICATION_ID
import com.nimith.echonote.presentation.common.NotificationHelper
import com.nimith.echonote.presentation.common.StorageUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    private var audioFocusRequest: AudioFocusRequest? = null

    private var silenceDetectionJob: Job? = null

    private var recordingStartTime: Long = 0
    private var timeWhenPaused: Long = 0
    private var lastAmplitude: Int = 0
    private var silenceStartTime: Long = 0

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                    TelephonyManager.EXTRA_STATE_RINGING,
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (isRecording) {
                            pauseRecording(isCall = true)
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (isRecording) {
                            resumeRecording(isCall = true)
                        }
                    }
                }
            }
        }
    }

    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
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

        val initialNotification = notificationHelper.createNotification("EchoNote is ready to record")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
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
            isRecording = true
            recordingStartTime = SystemClock.elapsedRealtime()
            audioRecorder.start()
            startSilenceDetection()
            isPausedByCall = false
            isPausedByFocusLoss = false
            updateNotification("Recording")
        }
    }

    private fun stop() {
        if (!isRecording) return

        isRecording = false

        lifecycleScope.launch {
            audioRecorder.stop()
        }
        releaseAudioFocus()
        stopSilenceDetection()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseRecording(isCall: Boolean = false) {
        if (!isRecording) return

        val wasPaused = isPausedByCall || isPausedByFocusLoss

        if (isCall) {
            isPausedByCall = true
        } else {
            isPausedByFocusLoss = true
        }

        // If it was already paused, just update the notification and return.
        if (wasPaused) {
            if (isPausedByCall) {
                updateNotification("Paused - Phone call")
            } else {
                updateNotification("Paused - Audio focus lost", addResumeAction = true)
            }
            return
        }

        audioRecorder.stop()
        timeWhenPaused = SystemClock.elapsedRealtime() - recordingStartTime
        stopSilenceDetection()

        if (isCall) {
            updateNotification("Paused - Phone call")
        } else {
            updateNotification("Paused - Audio focus lost", addResumeAction = true)
        }
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

        // If there is another reason to be paused, don't resume.
        if (isPausedByCall || isPausedByFocusLoss) {
            if (isPausedByCall) {
                updateNotification("Paused - Phone call")
            } else {
                updateNotification("Paused - Audio focus lost", addResumeAction = true)
            }
            return
        }

        if (!requestAudioFocus()) {
            Toast.makeText(this, "Could not regain audio focus.", Toast.LENGTH_SHORT).show()
            stop()
            return
        }

        audioRecorder.start()
        recordingStartTime = SystemClock.elapsedRealtime() - timeWhenPaused
        startSilenceDetection()
        updateNotification("Recording")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
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
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (isRecording) {
                    pauseRecording(isCall = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isRecording) {
                    pauseRecording(isCall = false)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isPausedByFocusLoss) {
                    resumeRecording(isCall = false)
                }
            }
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
        if (isRecording) {
            stop()
        }
        unregisterReceiver(phoneStateReceiver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }
}
