package com.example.myspeechy.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Job

interface LessonItemUiState {
    val lessonItem: LessonItem
}

data class RegularLessonItemState(
    override val lessonItem: LessonItem,
    val imgs: Map<String, ImageBitmap> = mapOf(),
    val textSplit: List<String> = listOf(),
    val supportedImgFormats: List<String> = listOf(".png")): LessonItemUiState

data class ReadingLessonItemState(
    override val lessonItem: LessonItem,
    val job: Job? = null,
    val index: Int = 0,
    var sliderPosition: Float = 1f
    ): LessonItemUiState

data class MeditationLessonItemState(
    override val lessonItem: LessonItem,
    val started: Boolean = false,
    val paused: Boolean = false,
    val breathingIn: Boolean = false,
    val setTime: Int = 0,
    val passedTime: Int = 0,
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