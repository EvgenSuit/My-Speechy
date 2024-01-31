package com.example.myspeechy.utils

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.LessonItem
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.data.MeditationLessonItemState
import com.example.myspeechy.services.MeditationLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeditationLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: MeditationLessonServiceImpl,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val id: Int = checkNotNull(savedStateHandle["meditationLessonItemId"])
    private val _uiState = MutableStateFlow(MeditationLessonItemState(LessonItem()))
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            lessonRepository.selectLessonItem(id).collect {lesson ->
                val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                _uiState.update { MeditationLessonItemState(lessonItem) }
            }
        }
    }

    fun markAsComplete() {
        lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
    }
}