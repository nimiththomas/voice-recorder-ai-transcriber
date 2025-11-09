package com.nimith.echonote.presentation.features.recording

sealed interface RecordingAction {
    data class Service(val action: String) : RecordingAction
}