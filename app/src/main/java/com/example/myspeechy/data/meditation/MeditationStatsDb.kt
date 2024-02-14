package com.example.myspeechy.data.meditation

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MeditationStats::class], version = 5)
abstract class MeditationStatsDb: RoomDatabase() {
    abstract fun meditationStatsDao(): MeditationStatsDao

    companion object {
        private var Instance: MeditationStatsDb? = null
        fun getDb(context: Context): MeditationStatsDb {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    MeditationStatsDb::class.java,
                    "meditationStatsDb"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}