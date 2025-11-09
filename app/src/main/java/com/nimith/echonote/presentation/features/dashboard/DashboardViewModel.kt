package com.nimith.echonote.presentation.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimith.echonote.domain.repository.ServiceStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serviceStateRepository: ServiceStateRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        // Sample data for preview
        val transcripts = mapOf(
            "Today" to listOf(
                TranscriptItem("1", "Meeting Notes", "10:00 AM", "5m"),
                TranscriptItem("2", "Lecture Summary", "11:30 AM", "30m")
            ),
            "Yesterday" to listOf(
                TranscriptItem("3", "Brainstorming Session", "2:00 PM", "15m")
            )
        )
        _state.update { it.copy(transcripts = transcripts) }
    }

    fun onStartNewRecording() {
        viewModelScope.launch {
            serviceStateRepository.clearServiceState()
        }
    }

    fun onDeleteTranscript(id: String) {
        val newTranscripts = _state.value.transcripts.toMutableMap()
        for ((date, transcriptList) in newTranscripts) {
            val updatedList = transcriptList.filterNot { it.id == id }
            if (updatedList.size != transcriptList.size) {
                if (updatedList.isEmpty()) {
                    newTranscripts.remove(date)
                } else {
                    newTranscripts[date] = updatedList
                }
                _state.update { it.copy(transcripts = newTranscripts) }
                break
            }
        }
    }
}
