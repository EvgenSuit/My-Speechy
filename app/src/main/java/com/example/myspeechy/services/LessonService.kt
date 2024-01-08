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
    val unit: Int,
    val category: String,
    val title: String,
    val text: String,
    val isComplete: Boolean,
    val containsImages: Boolean
)

interface LessonService {
    fun convertToLessonItem(lesson: Lesson): LessonItem
    suspend fun loadLesson(unit: Int, category: Int): Lesson

}

class LessonServiceImpl(private val lessonRepository: LessonRepository): LessonService {
    private val lessonServiceHelpers = LessonServiceHelpers()
    override fun convertToLessonItem(lesson: Lesson): LessonItem {
        return LessonItem(
            lesson.unit,
            lessonServiceHelpers.categoryMapper(lesson.category),
            lesson.title,
            lesson.text,
            lesson.isComplete == 1,
            lesson.containsImages == 1
        )
    }

    override suspend fun loadLesson(unit: Int, category: Int): Lesson {
        TODO("Not yet implemented")
    }

}