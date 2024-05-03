package com.myspeechy.myspeechy.presentation.thoughtTracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myspeechy.myspeechy.data.thoughtTrack.ThoughtTrack
import com.myspeechy.myspeechy.domain.DateFormatter
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.thoughtTracker.ThoughtTrackerItemService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThoughtTrackerItemViewModel @Inject constructor(
    private val thoughtTrackerItemService: ThoughtTrackerItemService,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val date: String = checkNotNull(savedStateHandle["date"])
    private val _uiState = MutableStateFlow(ThoughtTrackerItemUiState())
    val uiState = _uiState.asStateFlow()
    val trackFetchResultFlow = _uiState.map { it.trackFetchResult }
    val saveResultFlow = _uiState.map { it.saveResult }

    init {
        _uiState.update { it.copy(trackFetchResult = Result.InProgress) }
        thoughtTrackerItemService.listenForData(
            date = date,
            onError = {e ->
                      _uiState.update { it.copy(trackFetchResult
                      = Result.Error("Couldn't fetch track: ${e.name}") ) }
            },
            onData = {track ->
                if (track != null) {
                    _uiState.update { it.copy(track = track) }
                }
                _uiState.update { it.copy(trackFetchResult = Result.Success(""),
                    currentDate = DateFormatter.convertFromUtcThoughtTracker(date)
                ) }
            }
        )
    }
    fun saveData(questions: Map<String, Int>,
                         text: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(saveResult = Result.InProgress) }
                thoughtTrackerItemService.saveData(date, ThoughtTrack(questions, text))
                _uiState.update { it.copy(saveResult = Result.Success("")) }
            } catch (e: Exception) {
                _uiState.update { it.copy(saveResult =
                Result.Error("Couldn't save track: ${e.message!!}")) }
            }
        }
    }

    data class ThoughtTrackerItemUiState(val track: ThoughtTrack = ThoughtTrack(),
                                         val currentDate: String = "",
                                         val isDateEqualToCurrent: Boolean = false,
                                         val trackFetchResult: Result = Result.Idle,
                                         val saveResult: Result = Result.Idle)
}