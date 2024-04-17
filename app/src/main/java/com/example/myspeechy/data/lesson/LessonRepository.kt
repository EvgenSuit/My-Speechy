package com.example.myspeechy.data.lesson

import kotlinx.coroutines.flow.Flow

class LessonRepository(private val lessonDao: LessonDao): LessonDao {
    override suspend fun insertLesson(lesson: Lesson) =
        lessonDao.insertLesson(lesson)
    override suspend fun updateLesson(lesson: Lesson) =
        lessonDao.updateLesson(lesson)
    override fun selectAllLessons(): Flow<List<Lesson>> =
        lessonDao.selectAllLessons()
    override fun selectLessonItem(id: Int): Flow<Lesson> =
        lessonDao.selectLessonItem(id)

}