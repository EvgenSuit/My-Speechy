package com.example.myspeechy.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.R
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.utils.MainViewModel

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    Box {
        Image(painter = painterResource(id = R.drawable.main_page_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize())
        if (uiState.lessonItems.isNotEmpty()) {
            UnitColumn(lessonItems = uiState.lessonItems)
        }
    }
}

@Composable
fun UnitColumn(
    lessonItems: List<LessonItem>
) {
    val groupedItems = lessonItems.groupBy { it.unit }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(42.dp),
        modifier = Modifier.fillMaxSize()
        ) {
        items(groupedItems.keys.toList()) {columnIndex ->
            val columnItems = groupedItems[columnIndex]!!.toList()
            Column   {
                Text("Unit ${columnItems[0].unit}",
                    color = Color.White,
                    fontSize = 48.sp,
                    modifier = Modifier.padding(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(50.dp),
                    userScrollEnabled = true) {
                    items(columnItems) {
                        LessonItemComposable(lessonItem = it)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonItemComposable(lessonItem: LessonItem) {
    ElevatedButton(onClick = { /*TODO*/ },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        modifier = Modifier
            .size(191.dp, 130.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(lessonItem.category,
                fontSize = 20.sp)
            Spacer(modifier = Modifier.weight(0.2f))
            Text(lessonItem.title,
                fontSize = 30.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f))
        }
    }

}