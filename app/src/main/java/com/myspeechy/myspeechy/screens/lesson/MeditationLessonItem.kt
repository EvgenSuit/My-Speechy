package com.myspeechy.myspeechy.screens.lesson

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.LessonItemWrapper
import com.myspeechy.myspeechy.domain.AnimationConfig
import com.myspeechy.myspeechy.domain.meditation.MeditationConfig
import com.myspeechy.myspeechy.presentation.lesson.meditation.MeditationLessonItemViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.seconds

@Composable
fun MeditationLessonItem(viewModel: MeditationLessonItemViewModel = hiltViewModel(),
                         onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val passedTime = uiState.passedTime
    val context = LocalContext.current
    val breathingAnimationDuration = AnimationConfig.MEDITATION_BREATHING_ANIMATION_DURATION
    val started = uiState.started
    val paused = uiState.paused
    LaunchedEffect(Unit) {
        viewModel.saveResultFlow.collectLatest {res ->
            if (res.error.isNotEmpty()) {
                Toasty.error(context, res.error, Toast.LENGTH_SHORT, true).show()
            }
        }
    }
    LessonItemWrapper(
            uiState = uiState,
            onNavigateUp = onNavigateUp,
            dialogText = viewModel.onCategoryConvert(),
            onMarkAsComplete = viewModel::markAsComplete) {
            AnimatedVisibility(
                started,
                enter = expandVertically(tween(AnimationConfig.MEDITATION_TIMESCROLLER_ANIMATION_DURATION)),
                exit = shrinkVertically(tween(AnimationConfig.MEDITATION_TIMESCROLLER_ANIMATION_DURATION))
            ) {
                AnimatedContent(uiState.breathingIn,
                    transitionSpec = {
                        fadeIn(tween(breathingAnimationDuration)) togetherWith fadeOut(tween(breathingAnimationDuration))
                    }) { breathingIn ->
                    Text(
                        if (breathingIn) "Breath In" else "Breath out",
                        fontSize = 45.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 100.dp, bottom = 100.dp)
                    )
                }
            }
        if (started) {
            Text(
                passedTime.seconds.toString(),
                color = Color.White, fontSize = 35.sp
            )
        }
            TimeScroller(
                started,
                viewModel::setMeditationTime
            )
        Spacer(modifier = Modifier.height(40.dp))
        AnimatedContent(started,
            modifier = Modifier.absolutePadding(top = 20.dp)) { started ->
            if (!started) {
                ControlButton(stringResource(R.string.start_button)) {
                    viewModel.start()
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                ) {
                    ControlButton(if (paused) stringResource(R.string.resume_button)
                    else stringResource(R.string.pause_button)) {
                        if (paused) viewModel.resume() else viewModel.pause()
                    }
                    Spacer(Modifier.weight(1f))
                    ControlButton(stringResource(R.string.cancel_button)) {
                        viewModel.cancel()
                    }
                }

            }
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
    val timescrollerTextAnimationDuration = AnimationConfig.MEDITATION_TIMESCROLLER_TEXT_ANIMATION_DURATION
    val minMeditationPoint = MeditationConfig.MIN_MEDITATION_POINT
    val maxMeditationPoint = MeditationConfig.MAX_MEDITATION_POINT
    val stepSize = MeditationConfig.STEP_SIZE
    val corner = dimensionResource(R.dimen.common_corner_size)
    LaunchedEffect(started) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { list ->
                selectedItemIndex = if (list[list.size/2].index < maxMeditationPoint) list[list.size/2].index else selectedItemIndex
                onSetTime(selectedItemIndex * stepSize)
            }
    }
    AnimatedVisibility(visible = !started,
            enter = expandVertically(tween(AnimationConfig.MEDITATION_TIMESCROLLER_ANIMATION_DURATION)),
            exit = shrinkVertically(tween(AnimationConfig.MEDITATION_TIMESCROLLER_ANIMATION_DURATION))
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(corner))
                .background(Color.White.copy(0.2f))
                ) {
                    Text("Minutes", style = MaterialTheme.typography.bodyMedium)
                        LazyRow(
                            state = listState,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(120.dp)
                                .padding(5.dp)
                                .width(400.dp)
                                .testTag("TimeScroller")
                        ) {
                            items((minMeditationPoint..maxMeditationPoint).toList()) {
                                val fontSize = if (it == selectedItemIndex) 70 else 38
                                    Row(horizontalArrangement = Arrangement.spacedBy(50.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(
                                                start = if (it == minMeditationPoint + 1) 120.dp else 0.dp,
                                                end = if (it == maxMeditationPoint - 1) 120.dp else 0.dp)
                                            .width(120.dp)
                                    ) {
                                        if (it != 0 && it != maxMeditationPoint) {
                                            Text(
                                                "${it * stepSize}",
                                                fontSize = animateIntAsState(
                                                    targetValue = fontSize,
                                                    animationSpec = tween(
                                                        timescrollerTextAnimationDuration,
                                                        easing = Ease
                                                    ),
                                                    label = "textSize"
                                                ).value.sp,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier

                                            )
                                        }
                                        if (it != 0 && it != maxMeditationPoint - 1 && it != maxMeditationPoint) {
                                            Divider(
                                                color = MaterialTheme.colorScheme.background,
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
}

@Composable
fun ControlButton(text: String,
                      onClick: () -> Unit) {
    ElevatedButton(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .size(150.dp, 60.dp)
            .background(Color.Transparent),
        onClick = onClick
    ) {
        Text(
            text,
            fontSize = 23.sp
        )
    }
}