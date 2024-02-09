package com.example.myspeechy.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.MeditationStatsRepository
import com.example.myspeechy.services.MeditationStatsServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MeditationStatsViewModel @Inject constructor(
    private val meditationStatsRepository: MeditationStatsRepository,
    private val meditationStatsServiceImpl: MeditationStatsServiceImpl
): ViewModel() {
    private val _uiState = MutableStateFlow(MeditationStatsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
            meditationStatsServiceImpl.trackRemoteStats(date) {minutes ->
                _uiState.update {
                    val newMap = mapOf(date to minutes)
                    MeditationStatsUiState(newMap)
                }
            }
        }
    }
}

data class MeditationStatsUiState(val statsMap: Map<String, Int> = mapOf())