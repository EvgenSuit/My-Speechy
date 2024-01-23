package com.example.myspeechy.data

import androidx.compose.ui.graphics.ImageBitmap

interface UiState {
    val lessonItem: LessonItem
}

data class RegularLessonItemState(
    override val lessonItem: LessonItem,
    val imgs: Map<String, ImageBitmap> = mapOf(),
    val textSplit: List<String> = listOf(),
    val supportedImgFormats: List<String> = listOf(".png")): UiState

data class ReadingLessonItemState(override val lessonItem: LessonItem): UiState

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