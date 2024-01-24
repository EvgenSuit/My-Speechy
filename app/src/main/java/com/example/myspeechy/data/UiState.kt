package com.example.myspeechy.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Job

interface UiState {
    val lessonItem: LessonItem
}

data class RegularLessonItemState(
    override val lessonItem: LessonItem,
    val imgs: Map<String, ImageBitmap> = mapOf(),
    val textSplit: List<String> = listOf(),
    val supportedImgFormats: List<String> = listOf(".png")): UiState

data class ReadingLessonItemState(
    override val lessonItem: LessonItem,
    var colorIndices: List<Int> = listOf(),
    val changeColorIndicesJob: Job? = null,
    var sliderPosition: Float = 1f
    ): UiState

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