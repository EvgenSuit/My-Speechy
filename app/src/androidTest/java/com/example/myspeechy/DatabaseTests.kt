package com.example.myspeechy

import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.LessonRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

@HiltAndroidTest
class DatabaseTests {
    @Inject
    @Named("FakeLessonRepository")
    lateinit var fakeLessonRepository: LessonRepository

    @Inject
    @Named("FakeLessonDb")
    lateinit var fakeLessonDb: LessonDb

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        fakeLessonDb.close()
    }

    @Test
    fun writeReadTest() = runTest{
        val item = Lesson(id = 1, unit = 1, category = 2, title = "Introduction",
            text = "Some text", isComplete =  0, isAvailable = 0, containsImages = 0)
        fakeLessonRepository.insertLesson(item)
        val selectedItem = fakeLessonRepository.selectLessonItem(1).first()
        assertEquals(item.text, selectedItem.text)
    }
}

