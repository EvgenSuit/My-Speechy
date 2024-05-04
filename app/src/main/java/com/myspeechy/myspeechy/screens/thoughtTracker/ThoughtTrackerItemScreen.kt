package com.myspeechy.myspeechy.screens.thoughtTracker

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.BackButton
import com.myspeechy.myspeechy.components.ScrollDownButton
import com.myspeechy.myspeechy.domain.AnimationConfig
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.presentation.thoughtTracker.ThoughtTrackerItemViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ThoughtTrackerItemScreen(
    viewModel: ThoughtTrackerItemViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val mainPadding = dimensionResource(R.dimen.thought_tracker_page_padding)
    val maxThoughtTextLength = integerResource(R.integer.max_thought_length)
    var isQuestionsColumnVisible by rememberSaveable {
        mutableStateOf(true)
    }
    var questionsMap by rememberSaveable(uiState.track.questions) {
        mutableStateOf(uiState.track.questions)
    }
    var text by rememberSaveable(uiState.track.thoughts) {
        mutableStateOf(uiState.track.thoughts)
    }
    val isReadOnly by rememberSaveable(questionsMap, text, uiState.trackFetchResult) {
        mutableStateOf(uiState.trackFetchResult is Result.Success && questionsMap.values.all { it != -1 } &&
                uiState.track.thoughts != null)
    }
    LaunchedEffect(viewModel) {
        viewModel.trackFetchResultFlow.collect { result ->
            if (result.error.isNotEmpty()) {
                Toasty.error(context, result.error, Toast.LENGTH_SHORT, true).show()
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.saveResultFlow.collect { result ->
            if (result.error.isNotEmpty()) {
                Toasty.error(context, result.error, Toast.LENGTH_SHORT, true).show()
            }
        }
    }
    BackHandler {
        if (!isQuestionsColumnVisible) isQuestionsColumnVisible = true
        else onNavigateUp()
    }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                Modifier
                    .align(Alignment.Start)
                    .padding(top = mainPadding / 2, start = mainPadding / 2)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = {
                        //if in text section go back to questions.
                        //go back to main screen if there
                        if (!isQuestionsColumnVisible && !isReadOnly) {
                            isQuestionsColumnVisible = true
                        } else onNavigateUp()
                    })
                    if (isReadOnly) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f)) {
                            Text(uiState.currentDate,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                textAlign = TextAlign.Start)
                        }
                    }
                }
            }
            Box(contentAlignment = if (isQuestionsColumnVisible) Alignment.Center else Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.thought_tracker_page_padding))) {
                if (uiState.trackFetchResult is Result.Success) {
                    if (!isReadOnly) {
                        EditableTracker(
                            isQuestionsColumnVisible = isQuestionsColumnVisible,
                            questionsMap = questionsMap,
                            text = text ?: "",
                            saveResult = uiState.saveResult,
                            onQuestionMapEdit = {questionsMap = it},
                            onLastItemClick = { isQuestionsColumnVisible = false },
                            onTextChange = { if (it.length <= maxThoughtTextLength) text = it }) {
                            viewModel.saveData(questionsMap, text ?: "")
                        }
                    } else {
                        ReadOnlyTracker(
                            questionsMap = questionsMap,
                            text = text ?: "")
                    }
                } else if (uiState.trackFetchResult is Result.InProgress) {
                    CircularProgressIndicator()
                }
            }
        }
}

@Composable
fun ReadOnlyTracker(
    questionsMap: Map<String, Int>,
    text: String) {
    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels
    val listState = rememberLazyListState()
    var isBottomBarVisible by remember {
        mutableStateOf(true)
    }
    val areThoughtsVisible by remember(listState) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == 1 }
    }
    LaunchedEffect(Unit) {
        delay(AnimationConfig.BOTTOM_NAV_BAR_SHRINK_DURATION.toLong())
        isBottomBarVisible = false
    }
    val scope = rememberCoroutineScope()
    val padding = dimensionResource(R.dimen.thought_tracker_page_padding)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxHeight = this.maxHeight
        LazyColumn(state = listState,
            verticalArrangement = Arrangement.spacedBy(padding),
            horizontalAlignment = Alignment.CenterHorizontally) {
            item {
               Box {
                   QuestionsColumn(isReadOnly = true,
                       questionsMap = questionsMap,
                       onScoreChange = {},
                       onLastItemClick = {},
                       modifier = Modifier.height(maxHeight))
                   AnimatedVisibility(!areThoughtsVisible && text.isNotEmpty() && !isBottomBarVisible,
                       modifier = Modifier.align(Alignment.BottomCenter)) {
                       ScrollDownButton {
                           scope.launch {
                               listState.animateScrollToItem(1, scrollOffset = -screenHeight/2)
                           }
                       }
                   }
               }
            }
            if (text.isNotEmpty()) {
                item {
                    ThoughtsColumn(
                        isReadOnly = true,
                        text = text,
                        onTextChange = {},
                        onDoneClick = {},
                        modifier = Modifier
                            .height(maxHeight))
                }
            }
        }
    }
}

@Composable
fun EditableTracker(
    isQuestionsColumnVisible: Boolean,
    saveResult: Result,
    questionsMap: Map<String, Int>,
    text: String,
    onQuestionMapEdit: (Map<String, Int>) -> Unit,
    onLastItemClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onDoneClick: () -> Unit) {
    AnimatedContent(targetState = isQuestionsColumnVisible, label = "") { isVisible ->
        if (isVisible) {
            QuestionsColumn(
                isReadOnly = false,
                questionsMap = questionsMap,
                onScoreChange = {
                    onQuestionMapEdit(questionsMap.toMutableMap().apply { this[it.first] = it.second })
                },
                onLastItemClick = onLastItemClick)
        } else {
            ThoughtsColumn(
                isReadOnly = false,
                text = text,
                onTextChange = onTextChange,
                saveResult = saveResult,
                onDoneClick = onDoneClick)
        }
    }
}

@Composable
fun ThoughtsColumn(
    isReadOnly: Boolean,
    saveResult: Result? = null,
    text: String,
    onTextChange: (String) -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val width = dimensionResource(R.dimen.thought_tracker_width)
    val description = stringResource(R.string.thoughts_column)
    val textFieldDescription = stringResource(R.string.thoughts_text_field)
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .imePadding()
            .semantics { contentDescription = description }
            .fillMaxWidth()
            .run { if (!isReadOnly) this.verticalScroll(rememberScrollState()) else this }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.thoughts_padding)),
            modifier = Modifier
                .fillMaxHeight()
                .width(width)
        ) {
            if (!isReadOnly) Text(stringResource(R.string.write_down_thought_tracker_experience),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ))
            OutlinedTextField(
                enabled = !isReadOnly,
                value = text,
                textStyle = TextStyle(fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.kalam_regular))
                ),
                colors = TextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onBackground
                ),
                onValueChange = onTextChange,
                shape = RoundedCornerShape(dimensionResource(R.dimen.common_corner_size)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = textFieldDescription }
                    .imePadding()
            )
            if (!isReadOnly) {
                ElevatedButton(onClick = onDoneClick,
                    modifier = Modifier.size(dimensionResource(R.dimen.done_button_width),
                        dimensionResource(R.dimen.done_button_height))) {
                    Text(stringResource(R.string.done),
                        style = MaterialTheme.typography.bodyMedium
                            .copy(color = MaterialTheme.colorScheme.onBackground),
                        textAlign = TextAlign.Center)
                }
            }
            if (!isReadOnly && saveResult is Result.InProgress) {
                LinearProgressIndicator()
            }
        }
    }
}

@Composable
fun QuestionsColumn(
    isReadOnly: Boolean,
    questionsMap: Map<String, Int>,
    onScoreChange: (Pair<String, Int>) -> Unit,
    onLastItemClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val description = stringResource(R.string.questions_column)
    val width = dimensionResource(R.dimen.thought_tracker_width)
    LazyColumn(
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.questions_padding),
            Alignment.CenterVertically),
        modifier = modifier
            .semantics { contentDescription = description }
            .fillMaxSize()) {
        itemsIndexed(questionsMap.keys.toList()) {questionIndex, q ->
            AnimatedVisibility(visible = questionIndex <= questionsMap.values.filter { it != -1 }.size,
                enter = slideInVertically { -it }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(width)) {
                    Text(q, style = MaterialTheme.typography.bodyMedium
                        .copy(color = MaterialTheme.colorScheme.onBackground),
                        textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    val score = questionsMap[q]
                    LazyRow(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(15.dp,
                            Alignment.CenterHorizontally)) {
                        items((0..3).toList()) {i ->
                            //onPrimary: color for selected buttons
                            val containerColor = if (score != null && i <= score && score != -1) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                            ElevatedButton(
                                enabled = !isReadOnly,
                                onClick = {
                                   scope.launch {
                                       onScoreChange(Pair(q, i))
                                       //scroll down to account for a case where some items become invisible
                                       listState.animateScrollToItem(questionsMap.size)
                                       if (questionIndex == 3 || questionsMap.values.all { it != -1 }) onLastItemClick()
                                   } },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = containerColor,
                                    disabledContainerColor = containerColor.copy(0.7f)),
                                modifier = Modifier
                                    .size(dimensionResource(R.dimen.thought_tracker_ball_size))
                                    .testTag("Question button: $questionIndex $i")
                            ) {}
                        }
                    }
                }
            }
        }
    }
}