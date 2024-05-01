package com.myspeechy.myspeechy.thoughtTracker

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.testing.TestNavHostController
import com.myspeechy.myspeechy.MainActivity
import com.myspeechy.myspeechy.NavScreens
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.getInteger
import com.myspeechy.myspeechy.getString
import com.myspeechy.myspeechy.initTest
import com.myspeechy.myspeechy.onClickNodeWithContentDescription
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ThoughtTrackerItemUITests {
    @get: Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get: Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var navController: TestNavHostController

    @Before
    fun init() {
        with(composeRule) {
            navController = initTest(NavScreens.ThoughtTracker)
            onClickNodeWithContentDescription(NavScreens.ThoughtTracker.label)
            onNodeWithText("Today").performClick()
        }
    }

    @Test
    fun onQuestionsComplete_thoughtsColumnDisplayed() {
        fillQuestions(composeRule)
    }

    @Test
    fun onBackWhenThoughtsColumnIsDisplayed_questionsColumnDisplayed() {
        with(composeRule) {
            fillQuestions(this)
            onClickNodeWithContentDescription(getString(R.string.back_button))
            onNodeWithContentDescription(getString(R.string.questions_column)).assertIsDisplayed()
        }
    }

    @Test
    fun onThoughtsInput_lengthNotExceeded() {
        with(composeRule) {
            fillQuestions(this)
            val textFieldDescription = getString(R.string.thoughts_text_field)
            val maxLen = getInteger(R.integer.max_thought_length)
            val input = (1..maxLen+1).joinToString("") { "a" }
            for (c in input) {
                onNodeWithContentDescription(textFieldDescription).performTextInput(c.toString())
            }
            onNodeWithContentDescription(textFieldDescription).assertTextEquals((1..maxLen).joinToString("") { "a" })
        }
    }

    @Test
    fun onDoneClick_navigatedToThoughtTrackerScreen() {
        with(composeRule) {
            fillQuestions(this)
            onNodeWithContentDescription(getString(R.string.thoughts_text_field)).performTextInput("some thoughts")
            onNodeWithText(getString(R.string.done)).performClick()
            waitUntil { navController.currentBackStackEntry?.destination?.route?.
            equals(NavScreens.ThoughtTracker.route) == true }
        }
    }
}