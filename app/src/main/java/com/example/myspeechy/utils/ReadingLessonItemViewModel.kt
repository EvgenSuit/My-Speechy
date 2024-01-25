package com.example.myspeechy.utils

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.LessonItem
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.data.ReadingLessonItemState
import com.example.myspeechy.services.LessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.round

@HiltViewModel
class ReadingLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: LessonServiceImpl,
    savedStateHandle: SavedStateHandle): ViewModel(){
    private val id: Int = checkNotNull(savedStateHandle["readingLessonItemId"])
    private val _uiState = MutableStateFlow(ReadingLessonItemState(LessonItem()))
    val uiState = _uiState.asStateFlow()
    private val jobEnded = mutableStateOf(false)
    private val index = mutableIntStateOf(0)

    init {
        viewModelScope.launch {
            lessonRepository.selectLessonItem(id).collect {lesson ->
                val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                _uiState.update { ReadingLessonItemState(lessonItem,
                    List(lesson.text.length){0}) }
            }
        }
    }

    fun changeColorIndices(init: Boolean) {
        if (init) {
            _uiState.update {
                it.copy(colorIndices = List(it.lessonItem.text.length){0})
            }
        }
        _uiState.update { it1 -> it1.copy(changeColorIndicesJob = viewModelScope.launch {
            jobEnded.value = false
            val text = _uiState.value.lessonItem.text
            while (index.intValue < text.length) {
                delay((300 / it1.sliderPosition).toLong())
                _uiState.update {
                    it.copy(colorIndices = it.colorIndices.toMutableList().apply { this[index.intValue] = 1 })
                }
                index.intValue++
            }
        }) }
        _uiState.value.changeColorIndicesJob?.invokeOnCompletion {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    if (it == null) {
                        jobEnded.value = true
                        index.intValue = 0
                    }
                }
            }
        }
    }

    fun cancelJob(resetIndex: Boolean) {
        _uiState.value.changeColorIndicesJob?.cancel()
        _uiState.update {it.copy(changeColorIndicesJob = null)}
        if (resetIndex) {
            index.intValue = 0
        }
    }

    fun changeSliderPosition(newPosition: Float) {
        _uiState.update {
            it.copy(sliderPosition = newPosition)
        }
        cancelJob(false)
        changeColorIndices(jobEnded.value)
    }

    fun markAsComplete() {
        lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
    }
}