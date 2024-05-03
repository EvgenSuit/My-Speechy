package com.myspeechy.myspeechy.presentation.lesson.meditation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.meditation.MeditationStatsServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeditationStatsViewModel @Inject constructor(
    private val meditationStatsServiceImpl: MeditationStatsServiceImpl,
): ViewModel() {
    private val _uiState = MutableStateFlow(MeditationStatsUiState())
    val uiState = _uiState.asStateFlow()
    val loadResultFlow = _uiState.map { it.loadResult }

    init {
        viewModelScope.launch {
            delay(1000)
            _uiState.update { it.copy(moveGradientColors = false) }
        }
    }

    fun setUpListener(remove: Boolean) {
        viewModelScope.launch {
            updateLoadResult(Result.InProgress)
            meditationStatsServiceImpl.trackRemoteStats(
                coroutineScope = viewModelScope,
                remove = remove,
                { updateLoadResult(Result.Error(it.name)) },
                {updateLoadResult(Result.Error(it))}) { map ->
                val maxValue = if (map.values.isNotEmpty()) map.values.max() else 0
                _uiState.update {
                    it.copy(statsMap = map,
                        maxValue = if (map.values.isNotEmpty()) map.values.max() else 0,
                        //if max value of seconds is <= 60, build chart labels in seconds format, in minutes otherwise
                        labelList = if (maxValue <= 60) calculateInSeconds(maxValue) else calculateInMinutes(maxValue))
                }
                updateLoadResult(Result.Success(""))
            }
        }
    }
    private fun calculateInSeconds(maxValue: Int): List<Float> {
        val labelList = mutableListOf<Float>()
        for (i in 0..maxValue step 5) {
            labelList.add(i.toFloat())
        }
        return labelList
    }
    private fun calculateInMinutes(maxValue: Int): List<Float> {
        val labelList = mutableListOf<Float>()
        var currValue = 0f
        //get max number of minutes
        val max = maxValue.toFloat() / 60f
        while (currValue <= max) {
            labelList.add(currValue)
            //if max is <= 10 minutes, build chart labels in format: 1, 2, 3...min
            //else in format: 10, 20, 30...min
            currValue += if (max <= 10f) 1f else 10f
        }
        return labelList
    }

    private fun updateLoadResult(result: Result) {
        _uiState.update { it.copy(loadResult = result) }
    }
}

data class MeditationStatsUiState(val statsMap: Map<String, Int> = mapOf(),
                                  val maxValue: Int = 0,
                                  val labelList: List<Float> = listOf(),
                                  val moveGradientColors: Boolean = true,
    val loadResult: Result = Result.Idle)