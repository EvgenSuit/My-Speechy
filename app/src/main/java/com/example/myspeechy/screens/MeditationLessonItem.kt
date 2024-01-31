package com.example.myspeechy.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.components.LessonItemWrapper
import com.example.myspeechy.utils.MeditationLessonItemViewModel

@Composable
fun MeditationLessonItem(viewModel: MeditationLessonItemViewModel = hiltViewModel(),
                         onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    LessonItemWrapper(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onMarkAsComplete = { viewModel.markAsComplete() }) {

    }
}