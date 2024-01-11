package com.example.myspeechy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.jvm.Throws


@RunWith(AndroidJUnit4::class)
class MainScreenUITests {
    private lateinit var lessonRepository: LessonRepository
    private lateinit var lessonDb: LessonDb

    @Before
    fun initDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lessonDb = Room.inMemoryDatabaseBuilder(context, LessonDb::class.java)
            .build()
        lessonRepository = LessonRepository(lessonDb.lessonDao())
    }

    @After
    @Throws(IOException::class)
    fun cloeDb() {
        lessonDb.close()
    }

    @Test
    fun writeReadTest() = runTest{
        val item = Lesson(unit = 1, category = 2, title = "Introduction",
            text = "Some text", isComplete =  0, isAvailable = 0, containsImages =  0)
        lessonRepository.insertLesson(item)
        val readItem = lessonRepository.selectLessonItem(0).collect {
            assertEquals(item.text, it.text)
        }

    }


}