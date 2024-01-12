package com.example.myspeechy.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonFlagsRepository
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.services.LessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonFlagsRepository: LessonFlagsRepository,
    private val lessonServiceImpl: LessonServiceImpl
): ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

   init {
        viewModelScope.launch {
            val lessonList = lessonRepository.selectAllLessons().first().groupBy { it.unit }
                .values.toList().flatten()
            for (i in lessonList.indices) {
                if (i > 0) {
                    val prevLesson = lessonList[i-1]
                    val flag = lessonFlagsRepository.getFlag(prevLesson.id).first()
                    if (flag != null && flag.isComplete == 1) {
                        lessonRepository.insertLesson(lessonList[i].copy(isAvailable = 1))
                    }
                }
            }
            lessonRepository.selectAllLessons().collectLatest {lessonList ->
                _uiState.update {
                    UiState(lessonList.map {lesson -> lessonServiceImpl.convertToLessonItem(lesson) })
                } }
            }
        }

    fun getStringType(category: String): Int {
        return lessonServiceImpl.lessonServiceHelpers.categoryMapperReverse(category)
    }
    data class UiState(val lessonItems: List<LessonItem> = listOf())
}
