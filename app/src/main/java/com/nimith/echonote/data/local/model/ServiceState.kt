package com.nimith.echonote.data.local.model

data class ServiceState(
    val isRecording: Boolean,
    val recordingId: Long,
    val startTimeElapsed: Long
)
