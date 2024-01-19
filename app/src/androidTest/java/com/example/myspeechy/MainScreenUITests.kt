package com.example.myspeechy

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainScreenUITests {
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get: Rule(order = 1)
    val composeTestRule = createAndroidComposeRule(MainActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun markAsCompleteTest() {
        authUser(composeTestRule)
        composeTestRule.onNodeWithText("Some title 1").performClick()
        composeTestRule.onNodeWithText("Mark as complete").performClick()
        composeTestRule.onNodeWithTag("GoBackToMain").performClick()
        composeTestRule.onNodeWithText("Some title 2").assertIsEnabled()
    }
}