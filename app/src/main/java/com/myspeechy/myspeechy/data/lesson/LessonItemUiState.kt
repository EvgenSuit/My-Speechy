package com.myspeechy.myspeechy.data.lesson

import com.myspeechy.myspeechy.domain.Result

interface LessonItemUiState {
    val lessonItem: LessonItem
    val wasWelcomeDialogBoxShown: Boolean
}

data class RegularLessonItemState(
    override val lessonItem: LessonItem,
    override val wasWelcomeDialogBoxShown: Boolean = false
): LessonItemUiState

data class ReadingLessonItemState(
    override val lessonItem: LessonItem,
    override val wasWelcomeDialogBoxShown: Boolean = false,
    val started: Boolean = false,
    val paused: Boolean = false,
    val index: Int = 0,
    var sliderPosition: Float = 1f
    ): LessonItemUiState

data class MeditationLessonItemState(
    override val lessonItem: LessonItem,
    override val wasWelcomeDialogBoxShown: Boolean = false,
    val started: Boolean = false,
    val paused: Boolean = false,
    val breathingIn: Boolean = false,
    val setTime: Int = 0,
    val passedTime: Int = 0,
    val isNotificationCancelled: Boolean = false,
    val saveResult: Result = Result.Idle
): LessonItemUiState

data class LessonItem(
    var id: Int = 0,
    val unit: Int = 1,
    val category: LessonCategories = LessonCategories.NONE,
    val title: String = "",
    val text: String = "",
    val isComplete: Boolean = false,
    val isAvailable: Boolean = false,
    val containsImages: Boolean = false
)