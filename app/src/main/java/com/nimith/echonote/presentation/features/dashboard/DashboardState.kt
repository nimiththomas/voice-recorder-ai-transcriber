package com.nimith.echonote.presentation.features.dashboard

data class DashboardState(
    val transcripts: Map<String, List<TranscriptItem>> = emptyMap()
)

data class TranscriptItem(
    val id: String,
    val title: String,
    val time: String,
    val duration: String
)
