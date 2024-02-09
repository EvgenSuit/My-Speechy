package com.example.myspeechy.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myspeechy.utils.MeditationStatsViewModel

@Composable
fun MeditationStatsScreen(viewModel: MeditationStatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Stats")
        uiState.statsMap.entries.forEach { entry ->
            Text(entry.key)
            Text(entry.value.toString())
        }
    }
}