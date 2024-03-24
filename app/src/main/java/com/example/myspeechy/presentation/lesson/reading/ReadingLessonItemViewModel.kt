package com.example.myspeechy.presentation.lesson.reading

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.lesson.LessonItem
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.lesson.ReadingLessonItemState
import com.example.myspeechy.services.lesson.ReadingLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: ReadingLessonServiceImpl,
    savedStateHandle: SavedStateHandle): ViewModel(){
    private val id: Int = checkNotNull(savedStateHandle["readingLessonItemId"])
    private val _uiState = MutableStateFlow(ReadingLessonItemState(LessonItem()))
    val uiState = _uiState.asStateFlow()
    private val jobEnded = mutableStateOf(false)
    private lateinit var text: String

    init {
        viewModelScope.launch {
            lessonRepository.selectLessonItem(id).collect {lesson ->
                val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                text = lesson.text
                _uiState.update { ReadingLessonItemState(lessonItem) }
            }
        }
    }

    fun movePointer(init: Boolean) {
        if (init) {
            cancelJob(true)
        }
        _uiState.update { it1 -> it1.copy(job = viewModelScope.launch {
            jobEnded.value = false
            while (_uiState.value.index < text.length) {
                delay((300 / _uiState.value.sliderPosition).toLong())
                _uiState.update {
                    it.copy(index = it.index+1)
                }
            }
        }) }
        _uiState.value.job?.invokeOnCompletion {
            //If no error, than the job ended successfully
            if (it == null) {
                jobEnded.value = true
            }
        }
    }

    fun cancelJob(resetIndex: Boolean) {
        _uiState.value.job?.cancel()
        _uiState.update {it.copy(job = null,
            index = if (resetIndex) 0 else it.index)}
    }

    fun changeSliderPosition(newPosition: Float) {
        _uiState.update {
            it.copy(sliderPosition = newPosition)
        }
        cancelJob(false)
        movePointer(jobEnded.value)
    }

    fun markAsComplete() {
        lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
    }
}