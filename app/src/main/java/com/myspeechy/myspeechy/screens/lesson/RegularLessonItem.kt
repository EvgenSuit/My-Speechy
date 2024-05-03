package com.myspeechy.myspeechy.screens.lesson
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.myspeechy.myspeechy.components.LessonItemWrapper
import com.myspeechy.myspeechy.presentation.lesson.RegularLessonItemViewModel

@Composable
fun RegularLessonItem(viewModel: RegularLessonItemViewModel = hiltViewModel(),
                      onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    LessonItemWrapper(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        dialogText = viewModel.onCategoryConvert(),
        onDialogDismiss = viewModel::onDialogDismiss,
        onMarkAsComplete = { viewModel.markAsComplete() }) {
        Text(uiState.lessonItem.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                MaterialTheme.colorScheme.onBackground
            ),
            textAlign = TextAlign.Center)
    }
}