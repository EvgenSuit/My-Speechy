package com.myspeechy.myspeechy.presentation.lesson

import android.content.res.AssetManager
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myspeechy.myspeechy.data.lesson.LessonCategories
import com.myspeechy.myspeechy.data.lesson.LessonItem
import com.myspeechy.myspeechy.data.lesson.LessonRepository
import com.myspeechy.myspeechy.data.lesson.RegularLessonItemState
import com.myspeechy.myspeechy.domain.lesson.RegularLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegularLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: RegularLessonServiceImpl,
    savedStateHandle: SavedStateHandle): ViewModel() {
        private val id: Int = checkNotNull(savedStateHandle["id"])
        private val _uiState = MutableStateFlow(RegularLessonItemState(LessonItem()))
        val uiState = _uiState.asStateFlow()
        init {
            viewModelScope.launch {
                lessonRepository.selectLessonItem(id).collect { lesson ->
                    val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                       _uiState.update {
                           RegularLessonItemState(lessonItem)
                       }
                    collectDialogBoxShown()
                   }
            }
        }

    private fun collectDialogBoxShown() {
        viewModelScope.launch {
            lessonServiceImpl.collectDialogBoxShown(_uiState.value.lessonItem.category) {wasShown ->
                _uiState.update { it.copy(wasWelcomeDialogBoxShown = wasShown) }
            }
        }
    }
    fun onDialogDismiss() = viewModelScope.launch {
        lessonServiceImpl.editWelcomeDialogBoxShown(_uiState.value.lessonItem.category)
    }
    fun markAsComplete() {
        viewModelScope.launch {
            lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
        }
    }
    fun onCategoryConvert(): String = lessonServiceImpl.categoryToDialogText(uiState.value.lessonItem.category)
}

