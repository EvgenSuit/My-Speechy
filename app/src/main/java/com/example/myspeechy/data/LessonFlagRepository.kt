package com.example.myspeechy.data

import kotlinx.coroutines.flow.Flow

class LessonFlagsRepository(private val lessonFlagDao: LessonFlagsDao) : LessonFlagsDao{
    override suspend fun insertIsCompleteFlag(lessonFlags: LessonFlags) =
        lessonFlagDao.insertIsCompleteFlag(lessonFlags)

    override fun getFlag(id: Int): Flow<LessonFlags> =
        lessonFlagDao.getFlag(id)
}