package com.nimith.echonote.presentation.features.recording

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimith.echonote.domain.repository.RecordingRepository
import com.nimith.echonote.domain.repository.ServiceStateRepository
import com.nimith.echonote.presentation.common.Constants
import com.nimith.echonote.presentation.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordingViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    recordingStateHolder: RecordingStateHolder,
    private val recordingRepository: RecordingRepository,
    serviceStateRepository: ServiceStateRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingState())
    val state = _state.asStateFlow()

    private val _action = Channel<RecordingAction>()
    val action = _action.receiveAsFlow()

    private var isStarted = false

    init {
        val recordingId = savedStateHandle.get<String>("recordingId")
        if (recordingId == null) {
            onStart()

            recordingStateHolder.state
                .onEach { serviceState ->
                    _state.update {
                        val timer = DateUtils.formatMillis(serviceState.timerMillis)
                        it.copy(isRecording = serviceState.isRecording, timer = timer)
                    }
                }
                .launchIn(viewModelScope)

            serviceStateRepository.serviceState
                .map { it?.recordingId }
                .filterNotNull()
                .flatMapLatest { newRecordingId ->
                    recordingRepository.getRecording(newRecordingId)
                }
                .onEach { recording ->
                    _state.update {
                        it.copy(summary = recording?.summary ?: "")
                    }
                }
                .launchIn(viewModelScope)

            serviceStateRepository.serviceState
                .map { it?.recordingId }
                .filterNotNull()
                .flatMapLatest { newRecordingId ->
                    recordingRepository.getCompletedChunksForRecording(newRecordingId)
                }
                .onEach { chunks ->
                    _state.update {
                        val transcripts =
                            chunks.mapNotNull { chunk ->
                                chunk.transcription?.let {
                                    val time = DateUtils.formatTimestamp(chunk.createdAt)
                                    Transcript(it, time)
                                }
                            }
                        it.copy(transcripts = transcripts)
                    }
                }
                .launchIn(viewModelScope)
        } else {
            viewModelScope.launch {
                val recording =
                    recordingRepository.getRecording(recordingId.toLong()).filterNotNull().first()
                _state.update {
                    it.copy(
                        isRecording = false,
                        summary = recording.summary ?: "",
                        timer = DateUtils.formatMillis(recording.duration)
                    )
                }
            }
            recordingRepository.getChunksForRecording(recordingId.toLong())
                .onEach { chunks ->
                    _state.update {
                        val transcripts =
                            chunks.mapNotNull { chunk ->
                                chunk.transcription?.let {
                                    val time = DateUtils.formatTimestamp(chunk.createdAt)
                                    Transcript(it, time)
                                }
                            }
                        it.copy(transcripts = transcripts)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onEvent(action: RecordingAction) {
        when (action) {
            is RecordingAction.Service -> {
                viewModelScope.launch { _action.send(action) }
            }
        }
    }

    fun onStart() {
        if (isStarted) return
        isStarted = true
        onEvent(RecordingAction.Service(Constants.ACTION_START))
    }

    fun onStopRecording() {
        onEvent(RecordingAction.Service(Constants.ACTION_STOP))
    }
}