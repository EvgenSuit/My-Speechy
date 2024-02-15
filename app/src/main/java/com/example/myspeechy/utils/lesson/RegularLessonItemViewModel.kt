package com.example.myspeechy.utils.lesson

import android.content.res.AssetManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.lesson.LessonItem
import com.example.myspeechy.data.lesson.LessonRepository
import com.example.myspeechy.data.lesson.RegularLessonItemState
import com.example.myspeechy.services.lesson.RegularLessonServiceImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegularLessonItemViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val lessonServiceImpl: RegularLessonServiceImpl,
    private val assetManager: AssetManager,
    savedStateHandle: SavedStateHandle): ViewModel() {
        private val id: Int = checkNotNull(savedStateHandle["regularLessonItemId"])
        private val _uiState = MutableStateFlow(RegularLessonItemState(LessonItem()))
        val uiState = _uiState.asStateFlow()
        init {
            viewModelScope.launch {
                lessonRepository.selectLessonItem(id).collect { lesson ->
                    val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                   if (lessonItem.containsImages) {
                       //Load images
                       val dir = "imgs/unit${lessonItem.unit}/${lessonItem.category.lowercase()}/"
                       val imgs = assetManager.list(dir)!!.toList()
                       val textSplit = lessonServiceImpl.parseImgFromText(lessonItem, imgs)
                       val imgsMap = lessonServiceImpl.loadImgFromAsset(lessonItem, imgs, dir, assetManager)
                       _uiState.update {
                           RegularLessonItemState(lessonItem,
                               imgsMap,
                               textSplit)
                       }
                   } else {
                       _uiState.update {
                           RegularLessonItemState(lessonItem)
                       }
                   }
                }
            }
        }
    fun markAsComplete() {
        lessonServiceImpl.markAsComplete(_uiState.value.lessonItem)
    }
}

