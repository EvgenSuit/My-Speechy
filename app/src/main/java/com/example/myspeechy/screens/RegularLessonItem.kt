package com.example.myspeechy.screens
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.utils.RegularLessonItemViewModel
import kotlinx.coroutines.launch

@Composable
fun RegularLessonItem(id: Int,
                      viewModel: RegularLessonItemViewModel = hiltViewModel(),
                      onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutine = rememberCoroutineScope()

    RegularLessonItemBody(uiState, onNavigateUp){
        coroutine.launch {
            viewModel.markAsComplete()
        }
    }
}

@Composable
fun RegularLessonItemBody(uiState: RegularLessonItemViewModel.UiState,
                          onNavigateUp: () -> Unit,
                          onMarkAsComplete: () -> Unit) {
    val lessonItem = uiState.lessonItem
    val imgsMap = uiState.imgs
    val textSplit = uiState.textSplit
    val gradient = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color.Blue.copy(alpha = 0.5f),
            0.4f to Color.Blue.copy(alpha = 0.7f),
            1f to Color.Magenta.copy(alpha = 0.7f)
        )
    )


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(gradient)
            .verticalScroll(rememberScrollState())
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            GoBackButton(onNavigateUp,
                Modifier.align(Alignment.CenterVertically))
            Text(lessonItem.title, style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterVertically))
        }

        val text = buildAnnotatedString {
            for(split in textSplit) {
                if (split.contains(".png")) {
                    appendInlineContent(split)
                } else {
                    append(split)
                }
                append("\n")
            }
        }
        val inlineContent = mutableMapOf<String, InlineTextContent>()
        imgsMap.forEach { name, bitmap ->
            inlineContent.put(name,
                InlineTextContent(
                    Placeholder(width = 400.sp, height = 300.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline)
                ){
                    Image(bitmap = bitmap,
                        modifier = Modifier.fillMaxSize(),contentDescription = null)
                })
        }
            Text(text,
                style = MaterialTheme.typography.bodyMedium,
                inlineContent = inlineContent,
                modifier = Modifier.padding(10.dp))
            Spacer(modifier = Modifier.weight(1f))
            ElevatedButton(onClick = onMarkAsComplete) {
                Text("Mark as complete")
        }
    }
}
@Composable
fun GoBackButton(onClick: () -> Unit,
                 modifier: Modifier = Modifier) {
    IconButton(onClick = onClick,
        modifier = modifier
            .testTag("GoBackToMain")) {
        Icon(imageVector = Icons.Filled.ArrowBack,
            contentDescription = null)
    }
}