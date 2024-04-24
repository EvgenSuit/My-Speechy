package com.example.myspeechy.thoughtTracker

import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.myspeechy.MainActivity
import com.example.myspeechy.MySpeechyApp
import com.example.myspeechy.NavScreens
import com.example.myspeechy.R
import com.example.myspeechy.data.thoughtTrack.ThoughtTrack
import com.example.myspeechy.getInteger
import com.example.myspeechy.getString
import com.example.myspeechy.onClickNodeWithContentDescription
import com.example.myspeechy.signIn
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.max
import kotlin.random.Random

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
            activity.setContent {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())
                MySpeechyApp(navController)
            }
            if (Firebase.auth.currentUser == null) {
                waitUntilExactlyOneExists(hasText("My Speechy"), 5000)
                signIn(composeRule, navController)
            }
            waitUntilExactlyOneExists(hasContentDescriptionExactly(NavScreens.ThoughtTracker.label), 20*1000)
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