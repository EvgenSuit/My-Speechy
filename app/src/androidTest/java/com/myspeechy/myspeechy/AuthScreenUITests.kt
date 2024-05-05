package com.myspeechy.myspeechy

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.log

@HiltAndroidTest
@OptIn(ExperimentalTestApi::class)
class AuthScreenUITests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule(MainActivity::class.java)

    @get:Rule
    val hiltRule = HiltAndroidRule(this )
    private lateinit var navController: TestNavHostController

    @Before
    fun init() {
        Firebase.auth.signOut()
        composeTestRule.activity.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MySpeechyApp(navController)
        }
        composeTestRule.waitUntilExactlyOneExists(hasText("My Speechy"), 5000)
        composeTestRule.waitForIdle()
    }

    @Test
    fun invalidInputFormat_authUnavailable() {
        with(composeTestRule) {
            val logIn = getString(R.string.log_in)
            val signUp = getString(R.string.sign_up)
            onNode(hasText("Go to ${getString(R.string.sign_up)}")).assertIsDisplayed()
            onClickButtonWithLabel(getString(R.string.update_auth_option))
            onNode(hasClickLabel(logIn)).assertIsNotDisplayed()
            onUsernameInput("e")
            onUsernameInput("")
            assertTextIsDisplayed(getString(R.string.username_is_empty))

            onEmailInput("e")
            onEmailInput("")
            assertTextIsDisplayed(getString(R.string.email_is_empty))
            onEmailInput("incorrect format")
            assertTextIsDisplayed(getString(R.string.email_not_valid))

            onNode(hasClickLabel(signUp)).assertIsNotEnabled()

            onUsernameInput("username")
            onPasswordInput("e")
            onPasswordInput("")
            assertTextIsDisplayed(getString(R.string.password_is_empty))
            onPasswordInput("Okvn")
            assertTextIsDisplayed(getString(R.string.password_is_not_long_enough))
            onPasswordInput("fjkkdvnbd")
            assertTextIsDisplayed(getString(R.string.password_not_enough_digits))
            onPasswordInput("okvndfdkf2")
            assertTextIsDisplayed(getString(R.string.password_is_not_mixed_case))
            onNode(hasClickLabel(signUp)).assertIsNotEnabled()

            onClickButtonWithLabel(getString(R.string.update_auth_option))
            onNode(hasClickLabel(logIn)).assertIsNotEnabled()
            onNode(hasClickLabel(signUp)).assertIsNotDisplayed()
        }
    }

    @Test
    fun validInputFormat_authAvailable() {
        with(composeTestRule) {
            val logIn = getString(R.string.log_in)
            val signUp = getString(R.string.sign_up)
            onClickButtonWithLabel(getString(R.string.update_auth_option))
            onUsernameInput("username")
            onEmailInput("email@gmail.com")
            onPasswordInput("Fjfrkj434bg")
            onNode(hasClickLabel(signUp)).assertIsEnabled()
            onClickButtonWithLabel(getString(R.string.update_auth_option))
            onNode(hasClickLabel(logIn)).assertIsEnabled()
        }
    }

    @Test
    fun signInWrongCredentials_authFailed() {
        with(composeTestRule) {
            val logIn = getString(R.string.log_in)
            val signUp = getString(R.string.sign_up)
            onEmailInput("somerandomemail@gmail.com")
            onPasswordInput("Wrongpassword86")
            onClickButtonWithLabel(getString(R.string.log_in))
            onNode(hasClickLabel(logIn)).assertIsNotEnabled()
            onNode(hasClickLabel(signUp)).assertIsNotEnabled()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun signInCorrectCredentials_authSucceeded() {
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
            waitUntilExactlyOneExists(hasTestTag("loadScreen"), 3000)
            assertEquals(NavScreens.Main.route, navController.currentBackStackEntry?.destination?.route)
            waitUntilExactlyOneExists(hasTestTag("mainScreenContent"), 3000)
        }
    }
}