package com.nimith.echonote.presentation.features.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class RecordingServiceState(
    val isRecording: Boolean = false,
    val timerMillis: Long = 0L,
    val recordingId: Long? = null
)

@Singleton
class RecordingStateHolder @Inject constructor() {
    private val _state = MutableStateFlow(RecordingServiceState())
    val state = _state.asStateFlow()

    fun update(transform: (RecordingServiceState) -> RecordingServiceState) {
        _state.update(transform)
    }
}
