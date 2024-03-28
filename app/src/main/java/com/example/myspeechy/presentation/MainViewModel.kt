package com.example.myspeechy.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.lesson.Lesson
import com.example.myspeechy.data.lesson.LessonItem
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.loggedOutDataStore
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.lesson.MainLessonServiceImpl
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: MainLessonServiceImpl,
    private val authService: AuthService,
    @Named("AuthDataStore")
    private val authDataStore: DataStore<Preferences>
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

   init {
       //listenForAuthState()
       handleProgressLoading()
   }
    private fun listenForAuthState() {
        authService.listenForAuthState { isLoggedOut ->
            viewModelScope.launch {
                authDataStore.edit { loggedOutPref ->
                    loggedOutPref[loggedOutDataStore] = isLoggedOut
                }
            }
        }
    }
    fun handleProgressLoading() {
        _uiState.update { it.copy(dataState = FirestoreDataState.LOADING) }
        viewModelScope.launch {
            val lessonList = lessonRepository.selectAllLessons().first().groupBy { it.unit }
                .values.toList().flatten()
            lessonServiceImpl.trackRemoteProgress({errorCode ->
                if (Firebase.auth.currentUser != null) {
                    _uiState.update { it.copy(dataState = FirestoreDataState.ERROR,
                         errorCode = errorCode) }
                }
            }) { data ->
                //If error listening, the lesson list doesn't get changed
                var newLessonList = lessonList.map { lesson -> if (data.contains(lesson.id)) lesson.copy(isComplete = 1) else lesson.copy(isComplete = 0)}
                viewModelScope.launch {
                    newLessonList = handleAvailability(newLessonList)
                    _uiState.update {
                        UiState(newLessonList.map { lesson ->
                            lessonServiceImpl.convertToLessonItem(lesson)
                        }, dataState = FirestoreDataState.SUCCESS,
                            errorCode = FirebaseFirestoreException.Code.OK)
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
    fun logout() = authService.logOut()
    data class UiState(val lessonItems: List<LessonItem> = listOf(),
        val dataState: FirestoreDataState = FirestoreDataState.LOADING,
        val errorCode: FirebaseFirestoreException.Code = FirebaseFirestoreException.Code.OK)
}

enum class FirestoreDataState {
    LOADING,
    ERROR,
    SUCCESS
}