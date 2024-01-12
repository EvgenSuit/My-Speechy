package com.example.myspeechy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.example.myspeechy.services.LessonItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Insert
    suspend fun insertLesson(lesson: Lesson)

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Query("SELECT * FROM `database`")
    fun selectAllLessons(): Flow<List<Lesson>>
    @Query("SELECT * FROM `database` WHERE id = :id")
    fun selectLessonItem(id: Int): Flow<Lesson>

}