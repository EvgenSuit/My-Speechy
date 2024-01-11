package com.example.myspeechy.services

import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.helpers.LessonServiceHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

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

interface LessonService {
    fun convertToLessonItem(lesson: Lesson): LessonItem
    fun convertToLesson(lessonItem: LessonItem): Lesson
}

class LessonServiceImpl: LessonService {
    val lessonServiceHelpers = LessonServiceHelpers()
    override fun convertToLessonItem(lesson: Lesson): LessonItem {
        return LessonItem(
            lesson.id,
            lesson.unit,
            lessonServiceHelpers.categoryMapper(lesson.category),
            lesson.title,
            lesson.text,
            lesson.isComplete == 1,
            lesson.isAvailable == 1,
            lesson.containsImages == 1
        )
    }

    override fun convertToLesson(lessonItem: LessonItem): Lesson {
        return Lesson(
            lessonItem.id,
            lessonItem.unit,
            lessonServiceHelpers.categoryMapperReverse(lessonItem.category),
            lessonItem.title,
            lessonItem.text,
            if (lessonItem.isComplete) 1 else 0,
            if (lessonItem.isAvailable) 1 else 0,
            if (lessonItem.containsImages) 1 else 0
        )
    }

}