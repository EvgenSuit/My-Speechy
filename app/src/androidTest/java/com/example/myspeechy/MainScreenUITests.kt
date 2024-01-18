package com.example.myspeechy

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonDb
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.screens.AuthScreen
import com.example.myspeechy.screens.MainScreen
import com.example.myspeechy.screens.UnitColumn
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.services.LessonServiceImpl
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
import org.junit.runner.RunWith
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named


@HiltAndroidTest
class MainScreenUITests {
    @Inject
    @Named("FakeLessonRepository")
    lateinit var fakeLessonRepository: LessonRepository

    @Inject
    @Named("FakeLessonDb")
    lateinit var fakeLessonDb: LessonDb

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var composeTestRule = createComposeRule()

    //private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        /*composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MySpeechyApp(navController = navController)
        }*/
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

   /* @Test
    fun setupAppNavHost() {
        val items = listOf(
            LessonItem(id = 1, unit = 1, category = "", title = "Introduction",
                text = "Some text", isComplete = false, isAvailable = true, containsImages = false),
            LessonItem(id = 1, unit = 1, category = "", title = "Introduction 2",
                text = "Some text", isComplete = false, isAvailable = false, containsImages = false),
            LessonItem(id = 1, unit = 1, category = "", title = "Introduction 3",
                text = "Some text", isComplete = false, isAvailable = false, containsImages = false),
        )

    }*/

    /*@Test
    fun markAsCompleteTest() {
        composeTestRule.onNodeWithText("Introduction").performClick()
        composeTestRule.onNodeWithText("Mark as complete").performClick()
        composeTestRule.onNodeWithText("Introduction 2").assertIsDisplayed()
    }*/


}