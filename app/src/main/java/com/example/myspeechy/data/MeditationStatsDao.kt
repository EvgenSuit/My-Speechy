package com.example.myspeechy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationStatsDao {
    @Query("SELECT * FROM MeditationStats WHERE date = :date")
    fun getCurrentMeditationStats(date: String): Flow<MeditationStats>
    @Query("SELECT * FROM MeditationStats")
    fun getAllMeditationStats(): Flow<List<MeditationStats>>
    @Insert
    suspend fun insertMeditationStats(meditationStats: MeditationStats)

    @Update
    suspend fun updateMeditationStats(meditationStats: MeditationStats)

}