package com.example.myspeechy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

@OptIn(ExperimentalTestApi::class)
fun authUser(composeTestRule: ComposeTestRule) {
    composeTestRule.waitUntilExactlyOneExists(hasText("My Speechy"), 10000)
    composeTestRule.onNodeWithText("Email").performTextInput("some@gmail.com")
    composeTestRule.onNodeWithText("Password").performTextInput("SomePassword2")
    composeTestRule.onNodeWithText("Log In").performClick()
    composeTestRule.waitUntilExactlyOneExists(hasText("Unit 1"), 10000)
}
