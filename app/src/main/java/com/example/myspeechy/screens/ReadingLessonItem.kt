package com.example.myspeechy.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.components.LessonItemWrapper
import com.example.myspeechy.utils.ReadingLessonItemViewModel

@Composable
fun ReadingLessonItem(viewModel: ReadingLessonItemViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LessonItemWrapper(uiState = uiState,
        onNavigateUp = onNavigateUp,
        onMarkAsComplete = {viewModel.markAsComplete()}) {

    }
}