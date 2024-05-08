package com.myspeechy.myspeechy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.myspeechy.myspeechy.data.DataStoreManager
import com.myspeechy.myspeechy.data.lesson.Lesson
import com.myspeechy.myspeechy.data.lesson.LessonItem
import com.myspeechy.myspeechy.data.lesson.LessonRepository
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.domain.lesson.MainLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: MainLessonServiceImpl,
    private val dataStoreManager: DataStoreManager
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val resultFlow = _uiState.map { it.result }

   init {
       try {
           handleProgressLoading()
       } catch (e: Exception) {
           updateResult(Result.Error(e.message!!))
       }
   }
    private fun handleProgressLoading() {
        updateResult(Result.InProgress)
        viewModelScope.launch {
            dataStoreManager.editError("")
            dataStoreManager.showNavBar(false)
            dataStoreManager.onDataLoad(false)
            val lessonList = lessonRepository.selectAllLessons().first().groupBy { it.unit }
                .values.toList().flatten()
            lessonServiceImpl.trackRemoteProgress(
                coroutineScope = viewModelScope,
                {errorCode ->
                if (Firebase.auth.currentUser != null) {
                    updateResult(Result.Error(errorCode.name))
                    viewModelScope.launch {
                        dataStoreManager.editError(errorCode.name)
                        dataStoreManager.showNavBar(false)
                        dataStoreManager.onDataLoad(false)
                    }
                }
            },
                { updateResult(Result.Error(it)) }) { data ->
                //If error listening, the lesson list doesn't get changed
                var newLessonList = lessonList.map { lesson -> if (data.contains(lesson.id)) lesson.copy(isComplete = 1) else lesson.copy(isComplete = 0)}
                viewModelScope.launch {
                    newLessonList = handleAvailability(newLessonList)
                    val lessonItems = newLessonList.map { lesson ->
                        lessonServiceImpl.convertToLessonItem(lesson) }
                    if (_uiState.value.result !is Result.Success) {
                        dataStoreManager.editError("")
                        dataStoreManager.showNavBar(true)
                        dataStoreManager.onDataLoad(true)
                    }
                    _uiState.update {
                        UiState(lessonItems, result = Result.Success(FirebaseFirestoreException.Code.OK.name))
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
    private fun updateResult(result: Result) {
        _uiState.update { it.copy(result = result) }
    }
    data class UiState(val lessonItems: List<LessonItem> = listOf(),
                       val result: Result = Result.Idle)
}
