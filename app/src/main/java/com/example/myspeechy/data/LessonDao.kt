package com.example.myspeechy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Insert
    suspend fun insertLesson(lesson: Lesson)

    @Query("SELECT * FROM `database`")
    fun selectAllLessons(): Flow<List<Lesson>>
    /*@Query("SELECT * FROM lessonitem WHERE unit = :unit AND category = :category")
    fun selectLessonItem(unit: Int, category: Int): Flow<LessonItem>*/
}