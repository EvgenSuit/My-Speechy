package com.example.myspeechy.utils.lesson.meditation

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.meditation.MeditationStatsRepository
import com.example.myspeechy.services.meditation.MeditationStatsServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeditationStatsViewModel @Inject constructor(
    private val meditationStatsRepository: MeditationStatsRepository,
    private val meditationStatsServiceImpl: MeditationStatsServiceImpl,
    private val listenErrorToast: Toast
): ViewModel() {
    private val _uiState = MutableStateFlow(MeditationStatsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
             meditationStatsRepository.getAllMeditationStats().collect{loadedStats ->
                 meditationStatsServiceImpl.trackRemoteStats({ listenErrorToast.show() })
                 {map ->
                     val newMap = map.ifEmpty {
                         buildMap {
                             loadedStats.forEach {stats ->
                                 put(stats.date, stats.minutes)
                             }
                         }
                     }
                     _uiState.update {
                         MeditationStatsUiState(newMap)
                     }
                 }
            }
        }
    }
}

data class MeditationStatsUiState(val statsMap: Map<String, Int> = mapOf())