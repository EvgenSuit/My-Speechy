package com.example.myspeechy.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Job

interface LessonItemUiState {
    val lessonItem: LessonItem
}

data class RegularLessonItemStateLessonItem(
    override val lessonItem: LessonItem,
    val imgs: Map<String, ImageBitmap> = mapOf(),
    val textSplit: List<String> = listOf(),
    val supportedImgFormats: List<String> = listOf(".png")): LessonItemUiState

data class ReadingLessonItemStateLessonItem(
    override val lessonItem: LessonItem,
    val job: Job? = null,
    val index: Int = 0,
    var sliderPosition: Float = 1f
    ): LessonItemUiState

data class LessonItem(
    var id: Int = 0,
    val unit: Int = 1,
    val category: String = "",
    val title: String = "",
    val text: String = "",
    val isComplete: Boolean = false,
    val isAvailable: Boolean = false,
    val containsImages: Boolean = false
)