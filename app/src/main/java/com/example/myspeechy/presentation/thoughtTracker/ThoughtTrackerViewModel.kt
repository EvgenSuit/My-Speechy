package com.example.myspeechy.presentation.thoughtTracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.thoughtTrack.ThoughtTrackItem
import com.example.myspeechy.domain.DateFormatter
import com.example.myspeechy.domain.Result
import com.example.myspeechy.domain.thoughtTracker.ThoughtTrackerService
import com.example.myspeechy.domain.useCases.GetCurrentDateUseCase
import com.example.myspeechy.domain.useCases.IsDateEqualToCurrentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThoughtTrackerViewModel @Inject constructor(
    private val thoughtTrackerService: ThoughtTrackerService,
    private val isDateEqualToCurrentUseCase: IsDateEqualToCurrentUseCase,
    private val getCurrentDateUseCase: GetCurrentDateUseCase
): ViewModel() {
    private val _uiState = MutableStateFlow(ThoughtTrackerUiState())
    val uiState = _uiState.asStateFlow()
    private var dateListeningJob: Job? = null
    private var sourceTrackItems: List<ThoughtTrackItem> = mutableListOf()
    val tracksFetchResultFlow = _uiState.map { it.result }

    fun listenForTracks(remove: Boolean) {
        _uiState.update { it.copy(result = Result.InProgress) }
        thoughtTrackerService.listenForData(
            onError = { e ->
                _uiState.update { it.copy(result = Result.Error("Couldn't fetch tracks: ${e.name}")) }
            },
            onData = { trackItems ->
                val sortedTracks = trackItems.sortedByDescending { it.date }
                sourceTrackItems = sortedTracks
                _uiState.update {state ->
                    state.copy(trackItems = sortedTracks,
                        result = Result.Success("")) }
            },
            remove = remove
        )
    }
    fun listenForDateChange(cancel: Boolean) {
        if (cancel) {
            dateListeningJob?.cancel()
            return
        }
       dateListeningJob = viewModelScope.launch {
            while (true) {
                val tracks = uiState.value.trackItems
                if (tracks.isNotEmpty()) {
                    //count how many items have the same date, and if count is 0 it means that the current date
                    //is not equal to any of the dates in the list
                    val isDateDifferent = tracks.count { item ->
                        isDateEqualToCurrentUseCase(item.date)
                    } == 0
                    if (isDateDifferent) {
                        _uiState.update { it.copy(trackItems = (sourceTrackItems + ThoughtTrackItem(
                            date = getCurrentDateUseCase())).sortedByDescending { it.date }) }
                    }
                } else if (uiState.value.result is Result.Success) {
                    _uiState.update { it.copy(trackItems = listOf(ThoughtTrackItem(
                       date = getCurrentDateUseCase()))) }
                }
                delay(100)
            }
        }
    }

    fun formatDate(date: String): String = DateFormatter.convertFromUtcThoughtTracker(date)

    data class ThoughtTrackerUiState(val trackItems: List<ThoughtTrackItem> = listOf(),
                                     val result: Result = Result.Idle)
}