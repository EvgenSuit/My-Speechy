package com.example.myspeechy.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.components.LessonItemWrapper
import com.example.myspeechy.components.readingItemButtonBarGradient
import com.example.myspeechy.utils.ReadingLessonItemViewModel
import kotlinx.coroutines.Job
import kotlin.math.round

@Composable
fun ReadingLessonItem(viewModel: ReadingLessonItemViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val text = uiState.lessonItem.text
    val index = uiState.index
    val job: Job? = uiState.job
    var showDialog by remember {
        mutableStateOf(true)
    }

    Scaffold(
            bottomBar = {
                BottomAppBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .background(Color.Transparent)
                        .height(140.dp)
                        .border(1.dp, Color.Black),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .background(readingItemButtonBarGradient)
                            .fillMaxSize()
                            .padding(start = 50.dp, end = 50.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            ElevatedButton(
                                enabled = text.isNotEmpty(),
                                onClick = {
                                    viewModel.cancelJob(true)
                                    viewModel.movePointer(true)
                                },
                                modifier = Modifier.wrapContentSize(Alignment.Center)) {
                                Text("Start", fontSize = 23.sp, color = Color.Black)
                            }
                            /*If currently moving, cancel the job.
                                    If transitioning from stopped state,
                                    continue where previously left off*/
                            ElevatedButton(
                                enabled = text.isNotEmpty(),
                                onClick = {
                                    if (job != null) {
                                        viewModel.cancelJob(false)
                                    } else {
                                        viewModel.movePointer(false)
                                    }
                                },
                                modifier = Modifier.wrapContentSize(Alignment.Center)) {
                                Text(if (job == null) "Resume" else "Stop", fontSize = 23.sp,
                                    color = Color.Black)
                            }
                        }
                        Text("${round(uiState.sliderPosition * 10) / 10}x",
                            color = Color.White,
                            fontSize = 20.sp)
                        Slider(value = uiState.sliderPosition,
                            onValueChange = viewModel::changeSliderPosition,
                            valueRange = 1f..2f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Green
                            ),
                            modifier = Modifier.height(100.dp))
                    }
                }
            }
        ) {innerPadding ->
            LessonItemWrapper(uiState = uiState,
                onNavigateUp = onNavigateUp,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                dialogContent = {
                    Text("This is reading excercise", fontSize = 30.sp,
                        textAlign = TextAlign.Center)
                    Text("With this you can practice your slow reading skills." +
                            "This is a useful way to manage stuttering",
                        fontSize = 20.sp)},
                onMarkAsComplete = {viewModel.markAsComplete()}) {
                if (text.isNotEmpty()) {
                    val textStyle = MaterialTheme.typography.bodyMedium
                    val endText = buildAnnotatedString {
                        val colorfulSubstring = text.substring(0, index)
                        val uncolorfulSubstring = text.substring(index, text.length)
                        withStyle(style = SpanStyle(
                            color = Color.Cyan,
                            fontFamily = textStyle.fontFamily,
                            fontWeight = textStyle.fontWeight,
                            fontSize = textStyle.fontSize,
                        )
                        ) {
                            append(colorfulSubstring)
                        }
                        withStyle(style = SpanStyle(
                            color = Color.White,
                            fontFamily = textStyle.fontFamily,
                            fontWeight = textStyle.fontWeight,
                            fontSize = textStyle.fontSize,
                        )
                        ) {
                            append(uncolorfulSubstring)
                        }
                    }
                    Text(endText, style = TextStyle(lineHeight = textStyle.lineHeight))
                }
            }
    }
}