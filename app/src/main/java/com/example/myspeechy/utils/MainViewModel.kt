package com.example.myspeechy.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.services.LessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: LessonServiceImpl
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

   init {
        viewModelScope.launch {
            lessonRepository.selectAllLessons().collectLatest {lessonList ->
                _uiState.update { UiState(lessonList.map { lessonServiceImpl.convertToLessonItem(it) }) }
            }
        }
    }
    fun getStringType(category: String): Int {
        return lessonServiceImpl.lessonServiceHelpers.categoryMapperReverse(category)
    }
    data class UiState(val lessonItems: List<LessonItem> = listOf())
}
