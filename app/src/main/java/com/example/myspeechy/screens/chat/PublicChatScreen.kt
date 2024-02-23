package com.example.myspeechy.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.JoinButton
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.utils.chat.PublicChatViewModel
import kotlinx.coroutines.launch

@Composable
fun PublicChatScreen(navController: NavHostController,
                      viewModel: PublicChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var textFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.onPrimaryContainer)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BackButton(navController::navigateUp)
                Text(uiState.chat.title,
                    overflow = TextOverflow.Ellipsis)
            }
            MessagesColumn(viewModel.userId, listState, uiState.messages,
                LocalContext.current.filesDir.path, Modifier.weight(1f)){ chatId ->
                navController.navigate("chats/private/$chatId")
            }
            if (!uiState.joined) {
                JoinButton(viewModel::joinChat, Modifier)
            } else {
                BottomRow(textFieldValue, onFieldValueChange = {textFieldValue = it},
                    modifier = Modifier.height(IntrinsicSize.Min)) {
                    viewModel.sendMessage(textFieldValue)
                    textFieldValue = ""
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            }
        }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.startOrStopListening(true)
        }
    }
}