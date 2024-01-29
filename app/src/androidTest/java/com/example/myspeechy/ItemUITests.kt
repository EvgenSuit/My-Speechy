package com.example.myspeechy

import android.content.Context
import android.util.Log
import android.view.FrameMetrics.ANIMATION_DURATION
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.GestureScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
class ItemUITests {
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get: Rule(order = 1)
    val composeTestRule = createAndroidComposeRule(MainActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    //Run markAsCompleteTest and moveSliderTest separately!
    @Test
    fun markAsCompleteTest() {
        authUser(composeTestRule)
        composeTestRule.onNodeWithText("Some title 1").performClick()
        composeTestRule.onNodeWithText("Mark as complete").performClick()
        composeTestRule.onNodeWithTag("GoBackToMain").performClick()
        composeTestRule.onNodeWithText("Some title 2").assertIsEnabled()
    }

    @Test
    fun moveSliderTest() {
        authUser(composeTestRule)
        composeTestRule.onNodeWithText("Some title 1").performClick()
        composeTestRule.onNodeWithText("Mark as complete").performClick()
        composeTestRule.onNodeWithTag("GoBackToMain").performClick()
        composeTestRule.onNodeWithText("Some title 2").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("Slider")
            .performTouchInput { swipeRight() }
        composeTestRule.mainClock.advanceTimeBy(ANIMATION_DURATION.toLong() + 5L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.onNodeWithText("2.0x").assertIsDisplayed()
    }

    @Test
    fun parseImgInText() {
        val containsImgs = 1
        if (containsImgs != 1) return
        val unit = 1
        val category = "psychological"
        val text = "<Image>\n" +
                "Try to lift up your shoulders and tighten your stomach and lips. Is this feeling familiar to you?\n" +
                "<Image>\n" +
                "If yes, you could have stuttering."
        val textSplit = text.split("\n")
        var newText = ""
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imgFiles = context.assets.list("imgs/unit$unit/$category")!!.toList()
        var i = 0
        textSplit.forEach {
            if (it.contains("<Image>")) {
                newText += imgFiles[i]
                i++
            } else {
                newText += it
            }
        }
    }
}