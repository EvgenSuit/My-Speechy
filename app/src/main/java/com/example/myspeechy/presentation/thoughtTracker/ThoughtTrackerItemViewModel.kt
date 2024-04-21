package com.example.myspeechy.presentation.thoughtTracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.thoughtTrack.ThoughtTrack
import com.example.myspeechy.domain.Result
import com.example.myspeechy.domain.thoughtTracker.ThoughtTrackerItemService
import com.example.myspeechy.domain.useCases.GetCurrentDateInTimestampUseCase
import com.example.myspeechy.domain.useCases.IsDateEqualToCurrentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThoughtTrackerItemViewModel @Inject constructor(
    private val thoughtTrackerItemService: ThoughtTrackerItemService,
    private val isDateEqualToCurrentUseCase: IsDateEqualToCurrentUseCase,
    private val getCurrentDateInTimestampUseCase: GetCurrentDateInTimestampUseCase,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val timestamp: Long = checkNotNull(savedStateHandle["timestamp"])
    private val _uiState = MutableStateFlow(ThoughtTrackerItemUiState())
    val uiState = _uiState.asStateFlow()
    private var dateListeningJob: Job? = null
    val trackFetchResultFlow = _uiState.map { it.trackFetchResult }
    val saveResultFlow = _uiState.map { it.saveResult }

    init {
        listenForDateChange()
        _uiState.update { it.copy(trackFetchResult = Result.InProgress) }
        thoughtTrackerItemService.listenForData(
            timestamp = timestamp,
            onError = {e ->
                      _uiState.update { it.copy(trackFetchResult
                      = Result.Error("Couldn't fetch track: ${e.name}") ) }
            },
            onData = {track ->
                if (track != null) {
                    _uiState.update { it.copy(track = track) }
                }
                _uiState.update { it.copy(trackFetchResult = Result.Success(""),
                    currentTimestamp = timestamp) }
            }
        )
    }
    fun listenForDateChange() {
        if (dateListeningJob != null) {
            dateListeningJob?.cancel()
            return
        }
        dateListeningJob = CoroutineScope(Dispatchers.Main).launch {
            while(true) {
                _uiState.update { it.copy(currentTimestamp = getCurrentDateInTimestampUseCase(),
                    isDateEqualToCurrent = isDateEqualToCurrentUseCase(timestamp)) }
                delay(100)
            }
        }
    }
    suspend fun saveData(questions: Map<String, Int>,
                         text: String) {
        try {
            _uiState.update { it.copy(saveResult = Result.InProgress) }
            thoughtTrackerItemService.saveData(timestamp, ThoughtTrack(questions, text))
            _uiState.update { it.copy(saveResult = Result.Success("")) }
        } catch (e: Exception) {
            _uiState.update { it.copy(saveResult =
            Result.Error("Couldn't save track: ${e.message!!}")) }
        }
    }

    data class ThoughtTrackerItemUiState(val track: ThoughtTrack = ThoughtTrack(),
                                         val currentTimestamp: Long = 0,
                                         val isDateEqualToCurrent: Boolean = false,
                                         val trackFetchResult: Result = Result.Idle,
                                         val saveResult: Result = Result.Idle)
}