package com.example.myspeechy.utils

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.services.LessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegularLessonItemViewModel @Inject constructor(
        private val lessonRepository: LessonRepository,
        private val lessonServiceImpl: LessonServiceImpl,
        savedStateHandle: SavedStateHandle): ViewModel() {

        private val id: Int = checkNotNull(savedStateHandle["regularLessonItemId"])
        private val _uiState = MutableStateFlow(UiState())
        val uiState = _uiState.asStateFlow()
        init {
            viewModelScope.launch {
                lessonRepository.selectLessonItem(id).collect { lesson ->
                    _uiState.update { UiState(lessonServiceImpl.convertToLessonItem(lesson)) }
                }
            }
        }

        fun markAsComplete(lessonItem: LessonItem) {
            val lesson = lessonServiceImpl.convertToLesson(lessonItem.copy(isComplete = true))
            lessonServiceImpl.saveProgressRemotely(lesson.id)
        }

        data class UiState(val lessonItem: LessonItem = LessonItem())
}

