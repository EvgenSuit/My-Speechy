package com.example.myspeechy.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.utils.ChatsViewModel
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatsScreen(navController: NavHostController,
                viewModel: ChatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val chatsKeys = uiState.chats.keys.toList()
    val chatsValues = uiState.chats.values.toList()
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.chats.size) { i ->
            if (chatsValues[i] != null) {
                ElevatedButton(
                    onClick = { navController.navigate("chats/${chatsKeys[i]}") },
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(chatsValues[i]!!.title,
                            overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(SimpleDateFormat("hh:mm:ss").format(Date(chatsValues[i]!!.timestamp)),
                                modifier = Modifier.weight(0.5f))
                            Spacer(modifier = Modifier.weight(0.1f))
                            Text(chatsValues[i]!!.lastMessage,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.8f))
                        }
                    }
                }
            }
        }
    }
}