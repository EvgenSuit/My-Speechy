package com.example.myspeechy.screens
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.utils.RegularLessonItemViewModel
import kotlinx.coroutines.launch


@Composable
fun RegularLessonItem(id: Int,
                      onNavigateUp: () -> Unit) {
    val viewModel: RegularLessonItemViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val coroutine = rememberCoroutineScope()

    RegularLessonItemBody(lessonItem = uiState.lessonItem){
        coroutine.launch {
            viewModel.markAsComplete(uiState.lessonItem)
        }
    }
}

@Composable
fun RegularLessonItemBody(lessonItem: LessonItem,
                          onMarkAsComplete: () -> Unit) {
    val gradient = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color.Blue.copy(alpha = 0.5f),
            0.4f to Color.Blue.copy(alpha = 0.7f),
            1f to Color.Magenta.copy(alpha = 0.7f)
        )
    )


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(gradient)
    ) {
        Text(lessonItem.title, style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center)
        Text(lessonItem.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(10.dp))
        Spacer(modifier = Modifier.weight(1f))
        ElevatedButton(onClick = onMarkAsComplete) {
            Text("Mark as complete")
        }
    }
}