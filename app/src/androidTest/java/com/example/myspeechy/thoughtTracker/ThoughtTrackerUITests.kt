package com.example.myspeechy.thoughtTracker

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.myspeechy.MainActivity
import com.example.myspeechy.MySpeechyApp
import com.example.myspeechy.NavScreens
import com.example.myspeechy.R
import com.example.myspeechy.getString
import com.example.myspeechy.onClickNodeWithContentDescription
import com.example.myspeechy.signIn
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ThoughtTrackerUITests {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
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
            waitUntilAtLeastOneExists(hasContentDescriptionExactly(NavScreens.ThoughtTracker.label), 20*1000)
            onClickNodeWithContentDescription(NavScreens.ThoughtTracker.label)
        }
    }

    private fun onInitialDisplay() {
        with(composeRule) {
            onNodeWithText(getString(R.string.thought_tracker)).assertIsDisplayed()
            onNodeWithText("Today").assertIsDisplayed()
        }
    }
    @Test
    fun onInitialDisplay_todayIsShown() {
        onInitialDisplay()
    }

    @Test
    fun onTodayClick_navigatedToTrackerItem() {
        with(composeRule) {
            onInitialDisplay()
            onNodeWithText("Today").performClick()
            waitForIdle()
            navController.currentBackStackEntry?.destination?.route?.contains(NavScreens.ThoughtTracker.route+"/")
                ?.let { assertTrue(it) }
        }
    }


}