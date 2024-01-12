package com.example.myspeechy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonFlagsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIsCompleteFlag(lessonFlags: LessonFlags)

    @Query("SELECT * FROM LessonFlags WHERE id= :id")
    fun getFlag(id: Int): Flow<LessonFlags>
}