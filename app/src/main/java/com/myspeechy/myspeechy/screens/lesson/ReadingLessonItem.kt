package com.myspeechy.myspeechy.screens.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.LessonItemWrapper
import com.myspeechy.myspeechy.components.readingItemButtonBarGradient
import com.myspeechy.myspeechy.data.lesson.ReadingLessonItemState
import com.myspeechy.myspeechy.presentation.lesson.reading.ReadingLessonItemViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.round

@Composable
fun ReadingLessonItem(viewModel: ReadingLessonItemViewModel = hiltViewModel(),
                      onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val text = uiState.lessonItem.text
    var showControlBar by remember {
        mutableStateOf(true)
    }
    Box(Modifier.fillMaxSize()) {
        MainContent(
            uiState = uiState,
            scrollState = scrollState,
            dialogText = viewModel.onCategoryConvert(),
            onDialogDismiss = viewModel::onDialogDismiss,
            onMarkAsComplete = { if (!showControlBar) viewModel.markAsComplete() },
            onNavigateUp = onNavigateUp
        )
        if (!showControlBar) {
            ElevatedButton(onClick = { showControlBar = !showControlBar },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensionResource(R.dimen.show_control_bar_button_padding))
                    .height(dimensionResource(R.dimen.show_control_bar_button_height))) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
            }
        }
        AnimatedVisibility(showControlBar,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally) {
            ElevatedButton(onClick = { showControlBar = !showControlBar }) {
                Icon(if (showControlBar) Icons.Filled.ArrowDropDown
                    else Icons.Filled.KeyboardArrowUp, contentDescription = null)
            }
                ControlBar(started = uiState.started,
                    paused = uiState.paused,
                    isBarEnabled = text.isNotEmpty(),
                    sliderPosition = uiState.sliderPosition,
                    onStart = {
                         scope.launch {
                             viewModel.start()
                             scrollState.animateScrollTo(0)
                         }
                    },
                    onResumeOrStop = {
                        viewModel.pauseOrResume()
                    },
                    onSliderPositionChange = {
                        scope.launch {
                            viewModel.changeSliderPosition(it)
                            if (!uiState.started) {
                                scrollState.animateScrollTo(0)
                            }
                        }
                    },
                    modifier = Modifier.height(150.dp))
            }
        }
    }
}

@Composable
fun MainContent(
    uiState: ReadingLessonItemState,
    scrollState: ScrollState,
    dialogText: String,
    onDialogDismiss: () -> Unit,
    onMarkAsComplete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val text = uiState.lessonItem.text
    val textStyle = MaterialTheme.typography.bodySmall
    val index = uiState.index
    LessonItemWrapper(uiState = uiState,
        scrollState = scrollState,
        onNavigateUp = onNavigateUp,
        dialogText = dialogText,
        onDialogDismiss = onDialogDismiss,
        onMarkAsComplete = onMarkAsComplete,
        modifier = Modifier
            .fillMaxSize()) {
        AnimatedVisibility(text.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val endText = buildAnnotatedString {
                val readSubstring = text.substring(0, index)
                val unreadSubstring = text.substring(index, text.length)
                withStyle(style = SpanStyle(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = textStyle.fontFamily,
                    fontWeight = textStyle.fontWeight,
                    fontSize = textStyle.fontSize,
                )
                ) {
                    append(readSubstring)
                }
                withStyle(style = SpanStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = textStyle.fontFamily,
                    fontWeight = textStyle.fontWeight,
                    fontSize = textStyle.fontSize,
                )
                ) {
                    append(unreadSubstring)
                }
            }
            Text(endText, style = textStyle)
        }
    }
}

@Composable
fun ControlBar(started: Boolean,
    paused: Boolean,
    isBarEnabled: Boolean,
    sliderPosition: Float,
    onStart: () -> Unit,
    onResumeOrStop: () -> Unit,
    onSliderPositionChange: (Float) -> Unit,
    modifier: Modifier
) {
    val cornerSize = dimensionResource(R.dimen.common_corner_size)
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(topStart = cornerSize, topEnd = cornerSize))
            .background(readingItemButtonBarGradient)
            .height(150.dp)
            .padding(start = 50.dp, end = 50.dp)) {
        Row(
            horizontalArrangement = if (started) Arrangement.SpaceBetween else Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            ElevatedButton(
                enabled = isBarEnabled,
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.wrapContentSize(Alignment.Center)) {
                Text("Start", fontSize = 23.sp)
            }
            AnimatedVisibility(started,
                enter = slideInHorizontally { -it }) {
                ElevatedButton(
                    enabled = isBarEnabled,
                    onClick = onResumeOrStop,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                ) {
                    Text(if (paused) "Resume" else "Stop", fontSize = 23.sp)
                }
            }
        }
        Text("${round(sliderPosition * 10) / 10}x",
            color = Color.White,
            fontSize = 20.sp)
        Slider(value = sliderPosition,
            onValueChange = onSliderPositionChange,
            valueRange = 1f..2f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Green
            ),
            modifier = Modifier
                .height(100.dp)
                .testTag("Slider"))
    }
}