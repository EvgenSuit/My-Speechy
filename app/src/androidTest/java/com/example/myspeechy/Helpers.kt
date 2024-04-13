package com.example.myspeechy

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule


fun hasClickLabel(label: String) = SemanticsMatcher(label) {
    it.config.getOrNull(SemanticsActions.OnClick)?.label == label
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        assertTextIsDisplayed(text: String) {
    onNodeWithText(text).assertIsDisplayed()
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        getString(@StringRes id: Int): String = activity.getString(id)


fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        onClickButtonWithLabel(label: String) = onNode(hasClickLabel(label)).performClick()
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        onEmailInput(text: String) {
    onNode(hasClickLabel("Email")).performTextClearance()
    onNode(hasClickLabel("Email")).performTextInput(text)
        }

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        onPasswordInput(text: String) {
    onNode(hasClickLabel("Password")).performTextClearance()
    onNode(hasClickLabel("Password")).performTextInput(text)
        }

