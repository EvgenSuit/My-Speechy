package com.myspeechy.myspeechy.chat

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.myspeechy.myspeechy.MainActivity
import com.myspeechy.myspeechy.MySpeechyApp
import com.myspeechy.myspeechy.NavScreens
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.getString
import com.myspeechy.myspeechy.hasClickLabel
import com.myspeechy.myspeechy.hasLongClickLabel
import com.myspeechy.myspeechy.onClickButtonWithLabel
import com.myspeechy.myspeechy.onClickNodeWithContentDescription
import com.myspeechy.myspeechy.signIn
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ChatsScreenUiTests {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    private lateinit var navController: TestNavHostController
    private val newChatName = "new chat"
    @OptIn(ExperimentalTestApi::class)
    @Before
    fun init() {
        composeRule.activity.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MySpeechyApp(navController)
        }
        composeRule.waitForIdle()
        if (Firebase.auth.currentUser == null) {
            composeRule.waitUntilExactlyOneExists(hasText("My Speechy"), 5000)
            signIn(composeRule, navController)
        }
        composeRule.waitForIdle()
        composeRule.waitUntilExactlyOneExists(hasContentDescriptionExactly(NavScreens.ChatsScreen.label), 5000)
        composeRule.onClickNodeWithContentDescription(NavScreens.ChatsScreen.label)
    }

    @Test
    fun switchChatTypes_chatsShown() {
        with(composeRule) {
            val yourChats = getString(R.string.your_chats)
            val allGroups = getString(R.string.all_groups)
            onNodeWithContentDescription(yourChats).assertIsSelected()
            onNodeWithContentDescription(getString(R.string.add_public_chat)).assertIsNotDisplayed()

            onClickNodeWithContentDescription(allGroups)
            onNodeWithContentDescription(allGroups).assertIsSelected()
            onNodeWithContentDescription(getString(R.string.add_public_chat)).assertIsDisplayed()
        }
    }
    @Test
    fun createPublicChatIncorrectInput_chatNotCreated() {
        with(composeRule) {
            onClickNodeWithContentDescription(getString(R.string.all_groups))
            val addPublicChat = getString(R.string.add_public_chat)
            val dialog = getString(R.string.create_or_change_chat_form)
            val button = getString(R.string.create_or_change_chat_button)
            onClickNodeWithContentDescription(addPublicChat)
            onNodeWithContentDescription(dialog).assertIsDisplayed()

            onClickNodeWithContentDescription(button)

            onNodeWithContentDescription(dialog).assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun createPublicChatCorrectInput_chatCreated() {
        with(composeRule) {
            onClickNodeWithContentDescription(getString(R.string.all_groups))
            val addPublicChat = getString(R.string.add_public_chat)
            val dialog = getString(R.string.create_or_change_chat_form)
            val button = getString(R.string.create_or_change_chat_button)

            onClickNodeWithContentDescription(addPublicChat)
            onNodeWithContentDescription(getString(R.string.title)).requestFocus()
            onNodeWithContentDescription(getString(R.string.title)).performTextInput(newChatName)
            onNodeWithContentDescription(getString(R.string.title)).performImeAction()

            onNodeWithContentDescription(getString(R.string.description)).assertIsFocused()
            onNodeWithContentDescription(getString(R.string.description)).performTextInput("description")
            onNodeWithContentDescription(getString(R.string.description)).performImeAction()

            onClickNodeWithContentDescription(button)

            onNodeWithContentDescription(dialog).assertIsNotDisplayed()
            waitUntilExactlyOneExists(hasClickLabel(newChatName))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchForChat_chatShown() {
        with(composeRule) {
            val search = getString(R.string.search_for_public_chats)
            onClickNodeWithContentDescription(search)
            onClickNodeWithContentDescription(search).requestFocus()
            onNode(hasContentDescription(search)).performTextInput(newChatName)
            waitUntilExactlyOneExists(hasClickLabel(newChatName))
            onClickButtonWithLabel(newChatName)
            assertTrue(navController.currentBackStackEntry?.destination?.route.toString()
                    in "${NavScreens.ChatsScreen.route}/chats/public/")
        }
    }

    @Test
    fun clickOnAddChatThenFocusOnSearch_chatCreationCancelled() {
        with(composeRule) {
            val addPublicChat = getString(R.string.add_public_chat)
            val search = getString(R.string.search_for_public_chats)
            val form = getString(R.string.create_or_change_chat_form)
            onClickNodeWithContentDescription(getString(R.string.all_groups))
            onClickNodeWithContentDescription(addPublicChat)
            onNode(hasContentDescription(form)).assertIsDisplayed()
            onClickNodeWithContentDescription(search)
            onNode(hasContentDescription(form)).assertIsNotDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deletePublicChat_chatDeleted() {
        with(composeRule) {
            waitUntilExactlyOneExists(hasLongClickLabel("Long click: $newChatName"))
            onNode(hasLongClickLabel("Long click: $newChatName")).performSemanticsAction(SemanticsActions.OnLongClick)
            onNode(hasLongClickLabel("Long click: $newChatName")).assertIsDisplayed()
            onClickNodeWithContentDescription(getString(R.string.leave_chat))

            val isDialogDisplayed = onNode(hasContentDescription(getString(R.string.custom_alert_dialog)))
                .fetchSemanticsNode().layoutInfo.isPlaced

            if (isDialogDisplayed) {
                onClickNodeWithContentDescription(getString(R.string.custom_alert_dialog_confirm))
                onNode(hasContentDescription(getString(R.string.custom_alert_dialog))).assertIsNotDisplayed()
            }
            onNode(hasLongClickLabel("Long click: $newChatName")).assertIsNotDisplayed()
        }
    }
}