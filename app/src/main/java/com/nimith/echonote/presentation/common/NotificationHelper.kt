package com.nimith.echonote.presentation.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.nimith.echonote.R
import com.nimith.echonote.presentation.common.Constants.ACTION_RESUME
import com.nimith.echonote.presentation.common.Constants.ACTION_RESUME_TEXT
import com.nimith.echonote.presentation.common.Constants.ACTION_STOP
import com.nimith.echonote.presentation.common.Constants.ACTION_STOP_TEXT
import com.nimith.echonote.presentation.common.Constants.LIVE_UPDATES_CHANNEL_ID
import com.nimith.echonote.presentation.common.Constants.LIVE_UPDATES_CHANNEL_NAME
import com.nimith.echonote.presentation.common.Constants.NOTIFICATION_TITLE
import com.nimith.echonote.presentation.common.Constants.RECORDING_CHANNEL_DESCRIPTION
import com.nimith.echonote.presentation.common.Constants.RECORDING_CHANNEL_ID
import com.nimith.echonote.presentation.common.Constants.RECORDING_CHANNEL_NAME
import com.nimith.echonote.presentation.features.recording.RecordingService

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                RECORDING_CHANNEL_ID,
                RECORDING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = RECORDING_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(serviceChannel)

            if (Build.VERSION.SDK_INT >= 35) {
                val liveUpdatesChannel = NotificationChannel(
                    LIVE_UPDATES_CHANNEL_ID,
                    LIVE_UPDATES_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(liveUpdatesChannel)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun createLiveUpdateNotification(
        contentText: String,
        isRecording: Boolean,
        isPausedByCall: Boolean,
        isPausedByFocusLoss: Boolean,
        recordingStartTime: Long
    ): Notification {
        val stopIntent = Intent(context, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_launcher_foreground),
            ACTION_STOP_TEXT,
            stopPendingIntent
        ).build()

        val builder = Notification.Builder(context, LIVE_UPDATES_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(stopAction)
            .setOngoing(true)
            .setColorized(true)
            .setColor(Color.GRAY)

        if (isRecording && !isPausedByCall && !isPausedByFocusLoss) {
            builder.setChronometerCountDown(false)
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - recordingStartTime))
        }
        val notification = builder.build()
        notification.flags =
            notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        return notification
    }

    fun createNotification(
        contentText: String,
        addResumeAction: Boolean = false,
        recordingStartTime: Long = 0,
        isRecording: Boolean = false,
        isPausedByCall: Boolean = false,
        isPausedByFocusLoss: Boolean = false
    ): Notification {
        val stopIntent = Intent(context, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, RECORDING_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(R.drawable.ic_launcher_foreground, ACTION_STOP_TEXT, stopPendingIntent)
            .setOngoing(true)


        if (isRecording && !isPausedByCall && !isPausedByFocusLoss) {
            builder.setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - recordingStartTime))
                .setChronometerCountDown(false)
        }

        if (addResumeAction) {
            val resumeIntent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                context,
                1,
                resumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(R.drawable.ic_launcher_foreground, ACTION_RESUME_TEXT, resumePendingIntent)
        }

        val notification = builder.build()
        notification.flags =
            notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        return notification
    }

    fun buildNotificationForUpdate(
        contentText: String,
        addResumeAction: Boolean = false,
        isRecording: Boolean,
        isPausedByCall: Boolean,
        isPausedByFocusLoss: Boolean,
        recordingStartTime: Long
    ): Notification {
        return if (Build.VERSION.SDK_INT >= 36) { // Android 16+ (Baklava)
            createLiveUpdateNotification(
                contentText,
                isRecording,
                isPausedByCall,
                isPausedByFocusLoss,
                recordingStartTime
            )
        } else {
            createNotification(
                contentText,
                addResumeAction,
                recordingStartTime,
                isRecording,
                isPausedByCall,
                isPausedByFocusLoss
            )
        }
    }

}
