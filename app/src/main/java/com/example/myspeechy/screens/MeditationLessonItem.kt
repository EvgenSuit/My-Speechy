package com.example.myspeechy.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.components.LessonItemWrapper
import com.example.myspeechy.utils.MeditationLessonItemViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun MeditationLessonItem(viewModel: MeditationLessonItemViewModel = hiltViewModel(),
                         onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val passedTime = uiState.passedTime
    LessonItemWrapper(
            uiState = uiState,
            onNavigateUp = onNavigateUp,
            onMarkAsComplete = viewModel::markAsComplete
        ) {
            val started = uiState.started
            val paused = uiState.paused
            AnimatedVisibility(
                started,
                enter = expandVertically(tween(600)),
                exit = shrinkVertically(tween(600))
            ) {
                Text(
                    if (uiState.breathingIn) "Breath In" else "Breath out",
                    fontSize = animateIntAsState(
                        if (uiState.breathingIn) 105 else 45,
                        animationSpec = tween(uiState.breathingInterval.toInt(), easing = Ease),
                        label = ""
                    ).value.sp, color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 105.sp,
                    modifier = Modifier
                        .padding(top = 100.dp, bottom = 100.dp)
                        .height(IntrinsicSize.Min)
                )
            }
            Row(
                horizontalArrangement = if (started) Arrangement.SpaceBetween else Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                ElevatedButton(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(150.dp, 60.dp)
                        .background(Color.Transparent),
                    onClick = {
                        if (started) viewModel.cancel() else {
                            viewModel.start()
                        }
                    }
                ) {
                    Text(
                        if (started) "Cancel" else "Start",
                        fontSize = 23.sp
                    )
                }
                AnimatedVisibility(
                    visible = started,
                    enter = slideInHorizontally(animationSpec = tween(600)),
                    exit = shrinkHorizontally(animationSpec = tween(600))
                ) {
                    ElevatedButton(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(150.dp, 60.dp),
                        onClick = {
                            if (paused) viewModel.resume() else viewModel.pause()
                        }
                    ) {
                        Text(
                            if (paused) "Resume" else "Stop",
                            fontSize = 23.sp
                        )
                    }
                }
            }
            TimeScroller(
                started,
                viewModel::setMeditationTime
            )
            if (started) {
                Text(
                    passedTime.seconds.toString(),
                    color = Color.White, fontSize = 35.sp
                )
            }
        }
}

@Composable
fun TimeScroller(
    started: Boolean,
    onSetTime: (Int) -> Unit) {
    val listState = rememberLazyListState()
    var selectedItemIndex by remember {
        mutableIntStateOf(0)
    }
    LaunchedEffect(started) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { list ->
                selectedItemIndex = if (list[list.size/2].index < 13) list[list.size/2].index else selectedItemIndex
                onSetTime(selectedItemIndex*5)
            }
    }
    AnimatedVisibility(visible = !started,
            enter = expandVertically(tween(600)),
            exit = shrinkVertically(tween(600))
        ) {
        Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(0.2f))
                ) {
                    Text("Minutes", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(60.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(120.dp)
                            .padding(5.dp)
                            .testTag("TimeScroller")
                    ) {
                        items((0..13).toList()) {
                            if (it != 0 && it != 13) {
                                Text(
                                    "${it * 5}",
                                    fontSize = animateIntAsState(
                                        targetValue = if (it == selectedItemIndex) 70 else 38,
                                        animationSpec = tween(200, easing = Ease),
                                        label = "textSize"
                                    ).value.sp,
                                    color = Color.White, modifier = Modifier
                                        .padding(
                                            start = if (it == 1) (LocalConfiguration.current.screenWidthDp / 2).dp else 0.dp,
                                            end = if (it == 12) (LocalConfiguration.current.screenWidthDp / 2).dp else 0.dp
                                        )
                                )
                            }
                                Spacer(modifier = Modifier.width(60.dp))

                                if (it != 0 && it != 12 && it != 13) {
                                    Divider(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(1.dp)
                                    )
                                }

                        }
                    }
                }
            }
}