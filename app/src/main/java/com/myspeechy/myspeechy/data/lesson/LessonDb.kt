package com.myspeechy.myspeechy.data.lesson

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Lesson::class], version = 7, exportSchema = true)
abstract class LessonDb: RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    companion object {
        private var Instance: LessonDb? = null
        fun getDb(context: Context): LessonDb {
            return Instance ?: synchronized(this) {
                    Room.databaseBuilder(
                        context,
                        LessonDb::class.java,
                        "database.db"
                    ).createFromAsset("database/database.db")
                        .fallbackToDestructiveMigration()
                        .build()
                        .also { Instance = it }
                }

        }
    }
}