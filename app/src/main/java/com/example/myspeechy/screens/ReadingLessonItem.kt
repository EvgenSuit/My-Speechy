package com.example.myspeechy.screens

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.components.LessonItemWrapper
import com.example.myspeechy.utils.ReadingLessonItemViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.round

@Composable
fun ReadingLessonItem(viewModel: ReadingLessonItemViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val text = uiState.lessonItem.text
    val colorIndices = uiState.colorIndices
    val changeColorIndicesJob: Job? = uiState.changeColorIndicesJob
    Scaffold(
            bottomBar = {
                BottomAppBar(
                    actions = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(start = 50.dp, end = 50.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                ElevatedButton(
                                    enabled = text.isNotEmpty(),
                                    onClick = {
                                        viewModel.cancelJob(true)
                                        viewModel.changeColorIndices(true)
                                }) {
                                    Text("Start")
                                }
                                ElevatedButton(
                                    onClick = {
                                    if (changeColorIndicesJob != null) {
                                        viewModel.cancelJob(false)
                                    } else {
                                        viewModel.changeColorIndices(false)
                                    }
                                }) {
                                    Text(if (changeColorIndicesJob == null) "Resume" else "Stop")
                                }
                            }
                            Text("${round(uiState.sliderPosition * 10) / 10}")
                            Slider(value = uiState.sliderPosition,
                                onValueChange = viewModel::changeSliderPosition,
                                valueRange = 1f..2f)
                        }
                    },
                    modifier = Modifier.height(120.dp)
                )
            }
        ) {innerPadding ->
            LessonItemWrapper(uiState = uiState,
                onNavigateUp = onNavigateUp,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                onMarkAsComplete = {viewModel.markAsComplete()}) {
                if (text.isNotEmpty() && colorIndices.isNotEmpty()) {
                    val textStyle = MaterialTheme.typography.bodyMedium
                    val endText =  buildAnnotatedString {
                            for (i in text.indices) {
                                withStyle(style = SpanStyle(
                                    color = if (colorIndices[i] == 1) Color.Cyan else Color.White,
                                    fontFamily = textStyle.fontFamily,
                                    fontWeight = textStyle.fontWeight,
                                    fontSize = textStyle.fontSize,
                                )
                                ) {
                                    append(text[i])
                                }
                            }
                    }
                    Text(endText, style = TextStyle(lineHeight = textStyle.lineHeight))
                }
            }
    }
}