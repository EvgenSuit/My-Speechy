package com.example.myspeechy

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.myspeechy.screens.auth.AuthScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.log

@HiltAndroidTest
class AuthScreenUITests {
    private val wrongEmail = "somerandomemail@gmail.com"
    private val wrongPassword = "Wrongpassword85"

    @get:Rule
    val composeTestRule = createAndroidComposeRule(MainActivity::class.java)

    @get:Rule
    val hiltRule = HiltAndroidRule(this )
    private lateinit var navController: TestNavHostController

    @OptIn(ExperimentalTestApi::class)
    @Before
    fun init() {
        composeTestRule.activity.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AuthScreen {
                navController.navigate(NavScreens.Main.route)
            }
        }
        composeTestRule.waitUntilExactlyOneExists(hasText("My Speechy"), 5000)
        composeTestRule.waitForIdle()
    }

    @Test
    fun invalidInputFormat_authUnavailable() {

        with(composeTestRule) {
            val logIn = getString(R.string.log_in)
            val signUp = getString(R.string.sign_up)
            onEmailInput("e")
            onEmailInput("")
            assertTextIsDisplayed(getString(R.string.email_is_empty))
            onEmailInput("incorrect format")
            assertTextIsDisplayed(getString(R.string.email_not_valid))

            onNode(hasClickLabel(logIn)).assertIsNotEnabled()
            onNode(hasClickLabel(signUp)).assertIsNotEnabled()

            onPasswordInput("e")
            onPasswordInput("")
            assertTextIsDisplayed(getString(R.string.password_is_empty))
            onPasswordInput("Okvn")
            assertTextIsDisplayed(getString(R.string.password_is_not_long_enough))
            onPasswordInput("fjkkdvnbd")
            assertTextIsDisplayed(getString(R.string.password_not_enough_digits))
            onPasswordInput("okvndfdkf2")
            assertTextIsDisplayed(getString(R.string.password_is_not_mixed_case))
            onNode(hasClickLabel(logIn)).assertIsNotEnabled()
            onNode(hasClickLabel(signUp)).assertIsNotEnabled()
        }
    }

    @Test
    fun validInputFormat_authAvailable() {
        with(composeTestRule) {
            val logIn = getString(R.string.log_in)
            val signUp = getString(R.string.sign_up)
            onEmailInput("email@gmail.com")
            onPasswordInput("Fjfrkj434bg")
            onNode(hasClickLabel(logIn)).assertIsEnabled()
            onNode(hasClickLabel(signUp)).assertIsEnabled()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun signInWrongCredentials_authFailed() {
        with(composeTestRule) {
            val logIn = getString(R.string.log_in)
            val signUp = getString(R.string.sign_up)
            val waiting = getString(R.string.waiting_for_auth)
            onEmailInput(wrongEmail)
            onPasswordInput(wrongPassword)
            onClickButtonWithLabel(getString(R.string.log_in))
            waitForIdle()
            onNode(hasStateDescription(waiting)).assertIsDisplayed()
            waitForIdle()
            onNode(hasClickLabel(logIn)).assertIsNotDisplayed()
            onNode(hasClickLabel(signUp)).assertIsNotDisplayed()
            waitUntilExactlyOneExists(hasClickLabel(logIn))

            onNode(hasClickLabel(logIn)).assertIsNotEnabled()
            onNode(hasClickLabel(signUp)).assertIsNotEnabled()
        }
    }

}