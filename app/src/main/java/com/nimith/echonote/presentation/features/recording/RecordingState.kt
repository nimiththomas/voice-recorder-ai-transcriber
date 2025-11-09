package com.nimith.echonote.presentation.features.recording

data class RecordingState(
    val isLoading: Boolean = true,
    val timer: String = "00:00",
    val summary: String = "",
    val transcript: String = "",
    val actionItems: List<String> = emptyList()
)
