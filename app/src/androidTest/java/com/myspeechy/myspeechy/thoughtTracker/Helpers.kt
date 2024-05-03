package com.myspeechy.myspeechy.thoughtTracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.myspeechy.myspeechy.MainActivity
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.data.thoughtTrack.ThoughtTrack
import com.myspeechy.myspeechy.getString


fun fillQuestions(composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>) {
    with(composeRule) {
        onNodeWithContentDescription(getString(R.string.questions_column)).assertIsDisplayed()
        for (questionIndex in 0..<ThoughtTrack().questions.size) {
            onNodeWithTag("Question button: $questionIndex ${kotlin.random.Random.nextInt(0, 4)}").performClick()
        }
        onNodeWithContentDescription(getString(R.string.thoughts_column)).assertIsDisplayed()
    }
}