package com.example.myspeechy.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myspeechy.R
import com.example.myspeechy.data.LessonItem
import com.example.myspeechy.utils.MainViewModel

@Composable
fun MainScreen(navController: NavController,) {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    Box {
        Image(painter = painterResource(id = R.drawable.main_page_background_medium),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize())
        if (uiState.lessonItems.isNotEmpty()) {
            UnitColumn(
                lessonItems = uiState.lessonItems,
                navController
            ) { viewModel.getStringType(it) }
        }
    }
}

@Composable
fun UnitColumn(
    lessonItems: List<LessonItem>,
    navController: NavController,
    getStringType: (String) -> Int
) {
    val groupedItems = lessonItems.groupBy { it.unit }
    val keys = groupedItems.keys.toList()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(42.dp),
        modifier = Modifier.fillMaxSize()) {
        items(groupedItems.keys.size) {columnIndex ->
            val columnItems = groupedItems[keys[columnIndex]]!!.toList()
            Column{
                Text("Unit ${columnItems[0].unit}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(50.dp),
                    userScrollEnabled = true) {
                    items(columnItems.size) {rowIndex ->
                        val isAvailable = columnItems[rowIndex].isAvailable
                        LessonItemComposable(lessonItem = columnItems[rowIndex],
                            navController = navController,
                            isAvailable,
                            getStringType)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonItemComposable(lessonItem: LessonItem,
                         navController: NavController,
                         isAvailable: Boolean,
                         getStringType: (String) -> Int) {
    val itemType = when(getStringType(lessonItem.category)) {
        3 -> "meditationLessonItem"
        5 -> "readingLessonItem"
        else -> "regularLessonItem"
    }
    var titleFontSize by remember {
        mutableIntStateOf(30)
    }
    var readyToDraw by remember {
        mutableStateOf(false)
    }
    ElevatedButton(onClick = {navController.navigate("${itemType}/${lessonItem.id}")},
        shape = RoundedCornerShape(10.dp),
        enabled = isAvailable,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (lessonItem.isComplete) Color.Green else Color.White,
            contentColor = Color.Black,
            disabledContentColor = Color.White.copy(0.4f)
        ),
        modifier = Modifier.size(191.dp, 130.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(lessonItem.category,
                fontSize = 20.sp,
                )
            Spacer(modifier = Modifier.weight(0.2f))
            Text(lessonItem.title,
                fontSize = titleFontSize.sp,
                lineHeight = titleFontSize.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Visible,
                maxLines = 2,
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
}