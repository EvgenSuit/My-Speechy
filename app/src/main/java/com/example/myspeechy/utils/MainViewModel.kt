package com.example.myspeechy.utils

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.lesson.Lesson
import com.example.myspeechy.data.lesson.LessonItem
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.services.MainLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: MainLessonServiceImpl,
    private val listenErrorToast: Toast
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

   init {
       viewModelScope.launch {
           val lessonList = lessonRepository.selectAllLessons().first().groupBy { it.unit }
               .values.toList().flatten()
           lessonServiceImpl.trackRemoteProgress({ listenErrorToast.show() }) {data ->
               //If error listening, the lesson list doesn't get changed
               var newLessonList = lessonList.map { lesson ->if (data.contains(lesson.id)) lesson.copy(isComplete = 1) else lesson.copy(isComplete = 0)}
               viewModelScope.launch {
                   newLessonList = handleAvailability(newLessonList)
                   _uiState.update {
                       UiState(newLessonList.map { lesson ->
                           lessonServiceImpl.convertToLessonItem(lesson)
                       })
                   }
               }
           }
       }
   }

    private suspend fun handleAvailability(list: List<Lesson>): List<Lesson> {
        val lessonList = list.toMutableList()
        for (i in lessonList.indices) {
            if (i > 0) { lessonList[i] = lessonList[i].copy(isAvailable =
                    if(lessonList[i-1].isComplete == 1
                        || (i < lessonList.size - 1 && lessonList[i+1].isComplete == 1)
                        || lessonList[i].isComplete == 1) 1 else 0)

            }
            lessonRepository.insertLesson(lessonList[i])
        }
        return lessonList.toList()
    }

    fun getStringType(category: String): Int {
        return lessonServiceImpl.lessonServiceHelpers.categoryMapperReverse(category)
    }
    data class UiState(val lessonItems: List<LessonItem> = listOf())
}
