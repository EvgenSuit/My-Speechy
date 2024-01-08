package com.example.myspeechy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myspeechy.screens.AuthScreen

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule


@RunWith(AndroidJUnit4::class)
class AuthScreenUITests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun authScreenUncorrectTest() {
        composeTestRule.setContent {
            AuthScreen {}
        }
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
        composeTestRule.setContent {
            AuthScreen {}
        }
        composeTestRule.waitUntilExactlyOneExists(hasText("My Speechy"), 5000)
        composeTestRule.onNodeWithText("Email").performTextInput("some@gmail.com")
        composeTestRule.onNodeWithText("Wrong email format").assertDoesNotExist()
        composeTestRule.onNodeWithText("Log In").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Sign Up").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Password").performTextInput("SomePassword2")
        composeTestRule.onNodeWithText("Wrong password format").assertDoesNotExist()
        composeTestRule.onNodeWithText("Log In").assertIsEnabled()
        composeTestRule.onNodeWithText("Sign Up").assertIsEnabled()
    }
}