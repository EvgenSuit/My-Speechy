package com.example.myspeechy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AuthScreenUITests {
    @get:Rule
    val composeTestRule = createAndroidComposeRule(MainActivity::class.java)

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun authScreenUncorrectTest() {
        composeTestRule.waitUntilExactlyOneExists(hasText("My Speechy"), 5000)
        composeTestRule.onNodeWithText("Email").performTextInput("uncorrect format")
        composeTestRule.onNodeWithText("Wrong email format").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log In").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Sign Up").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Password").performTextInput(" ")
        composeTestRule.onNodeWithText("Wrong password format").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log In").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Sign Up").assertIsNotEnabled()

    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun authScreenCorrectTest() {
        composeTestRule.waitUntilExactlyOneExists(hasText("My Speechy"), 3000)
        composeTestRule.onNodeWithText("Email").performTextInput("some@gmail.com")
        composeTestRule.onNodeWithText("Wrong email format").assertDoesNotExist()
        composeTestRule.onNodeWithText("Log In").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Sign Up").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Password").performTextInput("SomePassword2")
        composeTestRule.onNodeWithText("Wrong password format").assertDoesNotExist()
        composeTestRule.onNodeWithText("Log In").assertIsEnabled()
        composeTestRule.onNodeWithText("Sign Up").assertIsEnabled()
        composeTestRule.onNodeWithText("Log In").performClick()
        composeTestRule.waitUntilExactlyOneExists(hasText("Unit 1"), 3000)
    }
}