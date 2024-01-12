package com.example.myspeechy

import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonFlags
import com.example.myspeechy.data.LessonFlagsDb
import com.example.myspeechy.data.LessonFlagsRepository
import com.example.myspeechy.data.LessonRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlin.jvm.Throws


@HiltAndroidTest
class MainScreenUITests {
    @Inject
    @Named("FakeLessonRepository")
    lateinit var fakeLessonRepository: LessonRepository

    @Inject
    @Named("FakeLessonDb")
    lateinit var fakeLessonDb: LessonDb

    @Inject
    @Named("FakeLessonFlagsRepository")
    lateinit var fakeLessonFlagsRepository: LessonFlagsRepository

    @Inject
    @Named("FakeLessonFlagsDb")
    lateinit var fakeLessonFlagsDb: LessonFlagsDb

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        fakeLessonDb.close()
        fakeLessonFlagsDb.close()
    }

    @Test
    fun writeReadTest() = runTest{
        val item = Lesson(id = 1, unit = 1, category = 2, title = "Introduction",
            text = "Some text", isComplete =  0, isAvailable = 0, containsImages = 0)
        fakeLessonRepository.insertLesson(item)
        val selectedItem = fakeLessonRepository.selectLessonItem(1).first()
        assertEquals(item.text, selectedItem.text)
    }

    @Test
    fun markAsCompleteTest() = runTest {
        val items = listOf(
            Lesson(id = 1, unit = 1, category = 0, title = "Introduction",
                text = "Some text", isComplete = 0, isAvailable = 1, containsImages = 0),
            Lesson(id = 2, unit = 1, category = 1, title = "Introduction",
                text = "Some text", isComplete = 0, isAvailable = 0, containsImages = 0),
            Lesson(id = 3, unit = 1, category = 1, title = "Introduction",
                text = "Some text", isComplete = 0, isAvailable = 0, containsImages = 0)
        )
        val firstItem = items[0]
        fakeLessonRepository.insertLesson(firstItem.copy(isComplete = 1))
        fakeLessonFlagsRepository.insertIsCompleteFlag(LessonFlags(id = firstItem.id))
        fakeLessonRepository.insertLesson(items[1])

        var newItems = fakeLessonRepository.selectAllLessons().first()
        for (i in newItems.indices) {
            if (i > 0) {
                val prevItem = newItems[i-1]
                val flag = fakeLessonFlagsRepository.getFlag(prevItem.id).first()
                if (flag.isComplete == 1) {
                    fakeLessonRepository.insertLesson(newItems[i].copy(isAvailable = 1))
                }
            }
        }
        newItems = fakeLessonRepository.selectAllLessons().first()
        assertTrue(newItems[1].isAvailable == 1)
    }


}