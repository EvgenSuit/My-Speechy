package com.example.myspeechy.data

import kotlinx.coroutines.flow.Flow

class LessonRepository(private val lessonDao: LessonDao): LessonDao {
    override suspend fun insertLesson(lesson: Lesson) =
        lessonDao.insertLesson(lesson)

    override fun selectAllLessons(): Flow<List<Lesson>> =
        lessonDao.selectAllLessons()
    /*override fun selectLessonItem(unit: Int, category: Int): Flow<LessonItem> =
        lessonDao.selectLessonItem(unit, category)*/
}