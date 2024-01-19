package com.example.myspeechy.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.services.LessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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
           collectRemoteProgress()
           lessonRepository.selectAllLessons().collectLatest { lessonList -> _uiState.update {
                           UiState(lessonList.map { lesson -> lessonServiceImpl.convertToLessonItem(lesson) }) }
           }
       }
   }

    private suspend fun collectRemoteProgress() {
        val lessonList = lessonRepository.selectAllLessons().first().groupBy { it.unit }
            .values.toList().flatten()
        lessonList.forEach { lesson ->
            lessonServiceImpl.trackRemoteProgress(lesson.id) { data ->
                val isComplete = if (data["isComplete"]!!) 1 else 0
                if (isComplete == 0) return@trackRemoteProgress
                val lessonIndex = lessonList.indexOf(lesson)
                        viewModelScope.launch {
                            if (lessonIndex < lessonList.size - 1) {
                                lessonRepository.insertLesson(
                                    lessonList[lessonIndex + 1].copy(
                                        isAvailable = isComplete
                                    )
                                )
                            }
                        }
            }
        }
    }

    fun getStringType(category: String): Int {
        return lessonServiceImpl.lessonServiceHelpers.categoryMapperReverse(category)
    }
    data class UiState(val lessonItems: List<LessonItem> = listOf())
}
