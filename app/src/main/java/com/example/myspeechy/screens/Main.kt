package com.example.myspeechy.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myspeechy.R
import com.example.myspeechy.services.LessonItem
import com.example.myspeechy.utils.MainViewModel

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    UnitColumn(lessonItems = uiState.lessonItems)
}

@Composable
fun UnitColumn(
    lessonItems: List<LessonItem>
) {
    LazyColumn() {
        items(lessonItems.size-1) {i ->
            if (lessonItems[i].unit != lessonItems[i+1].unit)
                Row {
                    LessonBody(lessonItem = lessonItems[i])
                }
        }
    }
}

@Composable
fun LessonBody(lessonItem: LessonItem) {
    Row(modifier = Modifier
        .horizontalScroll(rememberScrollState())) {
        
    }
}