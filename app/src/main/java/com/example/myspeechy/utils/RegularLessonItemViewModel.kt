package com.example.myspeechy.utils

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
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
        private val assetManager: AssetManager,
        savedStateHandle: SavedStateHandle): ViewModel() {
        private val id: Int = checkNotNull(savedStateHandle["regularLessonItemId"])
        private val _uiState = MutableStateFlow(UiState())
        val uiState = _uiState.asStateFlow()
        init {
            viewModelScope.launch {
                lessonRepository.selectLessonItem(id).collect { lesson ->
                    //Load images
                    val lessonItem = lessonServiceImpl.convertToLessonItem(lesson)
                    val imgsMap = mutableMapOf<String, ImageBitmap>()
                    var textSplit = listOf<String>()
                    if (lessonItem.containsImages) {
                        val dir = "imgs/unit${lessonItem.unit}/${lessonItem.category.lowercase()}/"
                        val imgsNames = assetManager.list(dir)!!.toList()
                        textSplit = lessonServiceImpl.parseImgFromText(lessonItem, imgsNames)

                        for (i in imgsNames.indices) {
                            imgsMap.put(imgsNames[i],
                                lessonServiceImpl.lessonServiceHelpers.loadImgFromAsset(dir + imgsNames[i], assetManager))
                        }
                    }
                    _uiState.update {
                        UiState(lessonItem,
                            imgsMap,
                            textSplit)
                    }

                }
            }
        }

        fun markAsComplete() {
            val lesson = lessonServiceImpl.convertToLesson(uiState.value.lessonItem.copy(isComplete = true))
            lessonServiceImpl.saveProgressRemotely(lesson.id)
        }

        data class UiState(val lessonItem: LessonItem = LessonItem(),
            val imgs: Map<String, ImageBitmap> = mapOf(),
            val textSplit: List<String> = listOf(),
            val supportedImgFormats: List<String> = listOf(".png")
        )
}

