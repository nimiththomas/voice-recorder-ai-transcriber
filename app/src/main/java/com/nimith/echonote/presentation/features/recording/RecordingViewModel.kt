package com.nimith.echonote.presentation.features.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimith.echonote.presentation.common.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    recordingStateHolder: RecordingStateHolder
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingState())
    val state = _state.asStateFlow()

    private val _action = Channel<RecordingAction>()
    val action = _action.receiveAsFlow()

    private var isStarted = false

    init {
        recordingStateHolder.state.onEach { serviceState ->
            _state.update {
                val timer = formatMillis(serviceState.timerMillis)
                it.copy(isLoading = serviceState.isRecording, timer = timer)
            }
        }.launchIn(viewModelScope)
    }

    fun onStart() {
        if (isStarted) return
        isStarted = true
        viewModelScope.launch {
            _action.send(RecordingAction.Service(Constants.ACTION_START))
        }
    }

    fun onStopRecording() {
        viewModelScope.launch {
            _action.send(RecordingAction.Service(Constants.ACTION_STOP))
        }
    }

    private fun formatMillis(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}
