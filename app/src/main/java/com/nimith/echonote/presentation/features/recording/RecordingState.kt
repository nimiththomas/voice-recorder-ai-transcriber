package com.nimith.echonote.presentation.features.recording

data class Transcript(
    val text: String,
    val time: String
)

data class RecordingState(
    val isRecording: Boolean = true,
    val isPaused: Boolean = false,
    val timer: String = "00:00",
    val summary: String = "",
    val transcripts: List<Transcript> = emptyList(),
    val actionItems: List<String> = emptyList()
)
