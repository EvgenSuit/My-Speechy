package com.example.myspeechy

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert


fun hasClickLabel(label: String) = SemanticsMatcher(label) {
    it.config.getOrNull(SemanticsActions.OnClick)?.label == label
}
fun hasLongClickLabel(label: String) = SemanticsMatcher(label) {
    it.config.getOrNull(SemanticsActions.OnLongClick)?.label == label
}

@OptIn(ExperimentalTestApi::class)
fun signIn(composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
           navController: TestNavHostController) {
    with(composeTestRule) {
        val logIn = getString(R.string.log_in)
        val signUp = getString(R.string.sign_up)
        val waiting = getString(R.string.waiting_for_auth)

        onEmailInput("some@gmail.com")
        onPasswordInput("Geny2005")
        onClickButtonWithLabel(getString(R.string.log_in))
        waitForIdle()
        onNode(hasStateDescription(waiting)).assertIsDisplayed()
        waitForIdle()
        onNode(hasClickLabel(logIn)).assertIsNotDisplayed()
        onNode(hasClickLabel(signUp)).assertIsNotDisplayed()
        waitUntilExactlyOneExists(hasTestTag(getString(R.string.load_screen)), 3000)
        Assert.assertEquals(
            NavScreens.Main.route,
            navController.currentBackStackEntry?.destination?.route
        )
        waitUntilExactlyOneExists(hasTestTag(getString(R.string.main_screen_content)), 3000)
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        assertTextIsDisplayed(text: String) {
    onNodeWithText(text).assertIsDisplayed()
}

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        getString(@StringRes id: Int): String = activity.getString(id)

fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.
        onClickNodeWithContentDescription(description: String) =
    onNodeWithContentDescription(description).performClick()

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

