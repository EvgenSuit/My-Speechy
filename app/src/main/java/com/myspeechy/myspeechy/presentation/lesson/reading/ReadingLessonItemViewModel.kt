package com.myspeechy.myspeechy.presentation.lesson.reading

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myspeechy.myspeechy.data.lesson.LessonCategories
import com.myspeechy.myspeechy.data.lesson.LessonItem
import com.myspeechy.myspeechy.data.lesson.LessonRepository
import com.myspeechy.myspeechy.data.lesson.ReadingLessonItemState
import com.myspeechy.myspeechy.domain.lesson.ReadingLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val id: Int = checkNotNull(savedStateHandle["id"])
    private val wasWelcomeDialogShown: Boolean = checkNotNull(savedStateHandle["wasWelcomeDialogShown"])
    private val _uiState = MutableStateFlow(ReadingLessonItemState(LessonItem()))
    val uiState = _uiState.asStateFlow()
    private var job: Job? = null
    private val jobEnded = mutableStateOf(false)

    init {
        viewModelScope.launch {
            lessonRepository.selectLessonItem(id).collect {lesson ->
                val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                _uiState.update { it.copy(
                    lessonItem = lessonItem,
                    wasWelcomeDialogBoxShown = wasWelcomeDialogShown
                ) }
            }
        }
    }


    private fun movePointer(init: Boolean) {
        if (init) {
            cancelJob(true)
        }
        _uiState.update { it.copy(started = true) }
        job = viewModelScope.launch {
            jobEnded.value = false
            while (_uiState.value.index < _uiState.value.lessonItem.text.length) {
                //the bigger sliderPosition the shorter the delay
                val duration = (90 / _uiState.value.sliderPosition).toLong()
                delay(duration)
                _uiState.update { it.copy(index = it.index+1) }
                configureDelayOnPunctuation()
            }
            _uiState.update { it.copy(started = false, paused = false) }
        }
        job?.invokeOnCompletion {
            //If no error, than the job ended successfully
            if (it == null) {
                jobEnded.value = true
            }
        }
    }

    private suspend fun configureDelayOnPunctuation() {
        val index = _uiState.value.index
        val text = _uiState.value.lessonItem.text
        if (index-1 < 0) return
        val prevChar = text[index-1]
        if (prevChar == '.') delay(1000)
        if (prevChar == ',') delay(400)
    }

    fun start() {
        _uiState.update { it.copy(paused = false) }
        cancelJob(true)
        movePointer(true)
    }

    fun pauseOrResume() {
        val paused = _uiState.value.paused
        cancelJob(false)
        _uiState.update { it.copy(paused = !paused) }
        if (paused) {
            movePointer(false)
        }
    }

    private fun cancelJob(resetIndex: Boolean) {
        job?.cancel()
        job = null
        /*If currently moving, cancel the job. If transitioning from stopped state,
              continue where previously left off*/
        _uiState.update {it.copy(index = if (resetIndex) 0 else it.index)}
    }

    fun changeSliderPosition(newPosition: Float) {
        _uiState.update { it.copy(sliderPosition = newPosition, paused = false) }
        cancelJob(false)
        movePointer(jobEnded.value)
    }


    fun markAsComplete() {
        viewModelScope.launch {
            lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
        }
    }
    fun onCategoryConvert(): String = lessonServiceImpl.categoryToDialogText(LessonCategories.READING)
}