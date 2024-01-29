package com.example.myspeechy.screens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.components.LessonItemWrapper
import com.example.myspeechy.utils.RegularLessonItemViewModel
import java.util.Locale

@Composable
fun RegularLessonItem(viewModel: RegularLessonItemViewModel = hiltViewModel(),
                      onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    LessonItemWrapper(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onMarkAsComplete = { viewModel.markAsComplete() }) {
        val imgsMap = uiState.imgs
        val textSplit = uiState.textSplit
        val text = buildAnnotatedString {
            for(split in textSplit) {
                if (uiState.supportedImgFormats.any { format -> split.contains(format) }) {
                    appendInlineContent(split)
                } else {
                    append(split)
                }
                append("\n")
            }
        }
        val inlineContent = mutableMapOf<String, InlineTextContent>()
        imgsMap.forEach { (name, bitmap) ->
            inlineContent[name] = InlineTextContent(
                    Placeholder(width = 400.sp, height = 300.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
                ){
                    Image(bitmap = bitmap,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        contentDescription = null)
                }
        }
        Text(text,
            style = MaterialTheme.typography.bodyMedium,
            inlineContent = inlineContent,
            textAlign = TextAlign.Center)
    }
}