package com.example.myspeechy.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonItem
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.services.MainLessonServiceImpl
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
    private val lessonServiceImpl: MainLessonServiceImpl
): ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

   init {
       viewModelScope.launch {
           collectRemoteProgress()
           lessonRepository.selectAllLessons().collectLatest { lessonList ->
               handleAvailability(lessonList)
               _uiState.update {
                           UiState(lessonList.map { lesson -> lessonServiceImpl.convertToLessonItem(lesson) }) }
           }
       }
   }

    private suspend fun collectRemoteProgress() {
        val lessonList = lessonRepository.selectAllLessons().first().groupBy { it.unit }
            .values.toList().flatten().toMutableList()
        for (lessonIndex in lessonList.indices){
            lessonServiceImpl.trackRemoteProgress(lessonList[lessonIndex].id) { data ->
                val isComplete = if (data["isComplete"]!!) 1 else 0
                if (isComplete == 0) return@trackRemoteProgress
                viewModelScope.launch {
                    if (lessonIndex < lessonList.size - 1) {
                        lessonList[lessonIndex+1] = lessonList[lessonIndex + 1].copy(
                            isAvailable = 1
                        )

                            lessonList[lessonIndex] = lessonList[lessonIndex].copy(
                                isComplete = 1
                            )
                            lessonRepository.insertLesson(
                                lessonList[lessonIndex]
                            )

                        lessonRepository.insertLesson(
                            lessonList[lessonIndex + 1]
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleAvailability(list: List<Lesson>) {
        val lessonList = list.toMutableList()
        for (i in lessonList.indices) {
            if (i > 0 && i < lessonList.size - 1) {
                if (lessonList[i].isAvailable == 0 &&
                    (lessonList[i-1].isComplete == 1 && lessonList[i+1].isComplete == 1)) {
                    lessonRepository.insertLesson(
                        lessonList[i].copy(
                            isAvailable = 1
                        )
                    )
                }
            }
        }
    }

    fun getStringType(category: String): Int {
        return lessonServiceImpl.lessonServiceHelpers.categoryMapperReverse(category)
    }
    data class UiState(val lessonItems: List<LessonItem> = listOf())
}
