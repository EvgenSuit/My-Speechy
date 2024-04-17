package com.example.myspeechy.data.meditation

import kotlinx.coroutines.flow.Flow

class MeditationStatsRepository(private val meditationStatsDao: MeditationStatsDao):
    MeditationStatsDao {
    override fun getCurrentMeditationStats(date: String): Flow<MeditationStats> {
        return meditationStatsDao.getCurrentMeditationStats(date)
    }

    override fun getAllMeditationStats(): Flow<List<MeditationStats>> {
        return meditationStatsDao.getAllMeditationStats()
    }

    override suspend fun insertMeditationStats(meditationStats: MeditationStats) {
        meditationStatsDao.insertMeditationStats(meditationStats)
    }
    override suspend fun updateMeditationStats(meditationStats: MeditationStats) {
        meditationStatsDao.updateMeditationStats(meditationStats)
    }
}