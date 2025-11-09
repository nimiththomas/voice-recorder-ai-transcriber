package com.nimith.echonote.presentation.common

object Constants {
    const val ACTION_START = "com.nimith.echonote.ACTION_START"
    const val ACTION_STOP = "com.nimith.echonote.ACTION_STOP"
    const val ACTION_PAUSE = "com.nimith.echonote.ACTION_PAUSE"
    const val ACTION_RESUME = "com.nimith.echonote.ACTION_RESUME"

    const val RECORDING_CHANNEL_ID = "RecordingChannel"
    const val RECORDING_CHANNEL_NAME = "Recording Channel"
    const val RECORDING_CHANNEL_DESCRIPTION = "Channel for the foreground recording service"
    const val TRANSCRIPTION_CHANNEL_ID = "TranscriptionChannel"
    const val TRANSCRIPTION_CHANNEL_NAME = "Transcription Channel"
    const val LIVE_UPDATES_CHANNEL_ID = "LiveUpdatesChannel"
    const val LIVE_UPDATES_CHANNEL_NAME = "Live Updates Channel"

    const val NOTIFICATION_ID = 1
    const val TRANSCRIPTION_NOTIFICATION_ID = 2

    const val NOTIFICATION_TITLE = "EchoNote Recording"

    const val ACTION_STOP_TEXT = "Stop"
    const val ACTION_RESUME_TEXT = "Resume"

    const val RECORDING_DIRECTORY = "recordings"
    const val RECORDING_FILE_PREFIX = "recording_"
    const val RECORDING_FILE_SUFFIX = ".mp4"

    const val BASE_URL = "https://api.openai.com/"

    const val MEDIA_TYPE_AUDIO = "audio/*"
    const val PART_NAME_FILE = "file"
    const val TRANSCRIPTION_MODEL = "whisper-1"
    const val MEDIA_TYPE_TEXT_PLAIN = "text/plain"
    const val AUTH_BEARER = "Bearer "
}
