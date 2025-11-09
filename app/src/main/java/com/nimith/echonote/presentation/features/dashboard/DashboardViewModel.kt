package com.nimith.echonote.presentation.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimith.echonote.domain.repository.RecordingRepository
import com.nimith.echonote.domain.repository.ServiceStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serviceStateRepository: ServiceStateRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recordingRepository.getRecordingsWithSummary()
                .map {
                    it.groupBy { recording ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = Date(recording.createdAt)
                        sdf.format(date)
                    }.mapValues { entry ->
                        entry.value.map {
                            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            val time = sdf.format(Date(it.createdAt))
                            TranscriptItem(
                                id = it.id.toString(),
                                title = it.summary ?: "",
                                time = time,
                                duration = "${it.duration / 1000}s"
                            )
                        }
                    }
                }
                .collect { transcripts ->
                    _state.update { it.copy(transcripts = transcripts) }
                }
        }
    }

    fun onStartNewRecording() {
        viewModelScope.launch {
            serviceStateRepository.clearServiceState()
        }
    }

    fun onDeleteTranscript(id: String) {
        viewModelScope.launch {
            val recording = recordingRepository.getRecording(id.toLong()).first()
            recording?.let {
                recordingRepository.deleteRecording(it)
            }
        }
    }
}
