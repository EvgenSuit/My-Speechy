package com.example.myspeechy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LessonFlags::class], version = 9)
abstract class LessonFlagsDb: RoomDatabase() {
    abstract fun lessonFlagsDao(): LessonFlagsDao

    companion object {
        private var Instance: LessonFlagsDb? = null
        fun getDb(context: Context): LessonFlagsDb {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    LessonFlagsDb::class.java,
                    "flags.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}