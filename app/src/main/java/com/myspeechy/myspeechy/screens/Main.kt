package com.myspeechy.myspeechy.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestoreException
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.data.lesson.LessonItem
import com.myspeechy.myspeechy.domain.Result
import com.myspeechy.myspeechy.presentation.MainViewModel
import es.dmoral.toasty.Toasty

@Composable
fun MainScreen(navController: NavHostController = rememberNavController(),
               viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.resultFlow.collect { result ->
            if (result is Result.Error && result.error != FirebaseFirestoreException.Code.NOT_FOUND.name) {
                Toasty.error(context, result.error, Toast.LENGTH_SHORT, true).show()
            }
        }
    }
    if (uiState.result is Result.Success) {
        Box(modifier = Modifier
            .fillMaxSize()
            .testTag(stringResource(R.string.main_screen_content))
            .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            if (uiState.lessonItems.isNotEmpty()) {
                UnitColumn(
                    lessonItems = uiState.lessonItems,
                    navController)
            }
        }
    }
    if (uiState.result is Result.InProgress) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .testTag(stringResource(R.string.load_screen))) {
            Text("My Speechy",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ))
        }
    }
}

@Composable
fun UnitColumn(
    lessonItems: List<LessonItem>,
    navController: NavController) {
    val groupedItems = lessonItems.groupBy { it.unit }
    val keys = groupedItems.keys.toList()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(42.dp),
        modifier = Modifier.fillMaxSize()) {
        items(groupedItems.keys.size) {columnIndex ->
            val columnItems = groupedItems[keys[columnIndex]]!!.toList()
            Column {
                Text("Unit ${columnItems[0].unit}",
                    style = MaterialTheme.typography.titleLarge
                        .copy(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.padding(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(50.dp),
                    userScrollEnabled = true) {
                    items(columnItems.size) {rowIndex ->
                        val isAvailable = columnItems[rowIndex].isAvailable
                        LessonItemComposable(lessonItem = columnItems[rowIndex],
                            navController = navController,
                            isAvailable)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonItemComposable(lessonItem: LessonItem,
                         navController: NavController,
                         isAvailable: Boolean) {
    val description = stringResource(R.string.lesson_item)
    var titleFontSize by remember {
        mutableIntStateOf(30)
    }
    var readyToDraw by remember {
        mutableStateOf(false)
    }
    ElevatedButton(onClick = {navController.navigate("${lessonItem.category}/${lessonItem.id}") {
        launchSingleTop = true
    } },
        shape = RoundedCornerShape(10.dp),
        enabled = isAvailable,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (lessonItem.isComplete) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
            contentColor = Color.Black,
            disabledContentColor = Color.White.copy(0.4f)
        ),
        modifier = Modifier
            .size(300.dp, 200.dp)
            .semantics { contentDescription = "$description: ${lessonItem.category}" }
    ) {
            Text(lessonItem.title,
                style = MaterialTheme.typography.labelMedium
                    .copy(if (lessonItem.isComplete) MaterialTheme.colorScheme.onPrimary else Color.Black),
                onTextLayout = {res ->
                    //Dynamic font size
                    if (res.didOverflowWidth || res.didOverflowHeight) {
                        titleFontSize = (titleFontSize * 0.9).toInt()
                    } else {
                        readyToDraw = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .drawWithContent { if (readyToDraw) drawContent() })
    }
}