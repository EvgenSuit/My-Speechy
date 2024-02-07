package com.example.myspeechy

import android.content.Context
import android.view.FrameMetrics.ANIMATION_DURATION
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    fun breathingLessonTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.text("Allow")), 10000)
        val allowButton = device.findObject(UiSelector().text("Allow"))
        allowButton.click()
        authUser(composeTestRule)
        composeTestRule.onNodeWithText("Meditation").performClick()
        composeTestRule.onNodeWithText("Mark as complete").performClick()
        composeTestRule.onNodeWithTag("TimeScroller", useUnmergedTree = true)
            .performTouchInput { swipeLeft() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.mainClock.advanceTimeBy(ANIMATION_DURATION.toLong() + 5L)
        composeTestRule.onNodeWithText("Start", useUnmergedTree = true).performClick()
        composeTestRule.mainClock.advanceTimeBy(ANIMATION_DURATION.toLong() + 5L)
        composeTestRule.onNodeWithTag("TimeScroller", useUnmergedTree = true).assertDoesNotExist()

        device.openNotification()
        device.wait(Until.hasObject(By.text("Meditation")), 10000)
        val title = device.findObject(UiSelector().text("Meditation"))
        assertTrue(title.exists())
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