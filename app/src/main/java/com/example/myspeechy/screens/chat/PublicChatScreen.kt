package com.example.myspeechy.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.JoinButton
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.utils.chat.PublicChatViewModel

@Composable
fun PublicChatScreen(navController: NavHostController,
                      viewModel: PublicChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    Box {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BackButton(navController::navigateUp)
                Text(uiState.chat.title,
                    overflow = TextOverflow.Ellipsis)
            }
            MessagesColumn(viewModel.userId, uiState.messages){chatId ->
                navController.navigate("chats/private/$chatId")
            }
        }
        if (!uiState.joined) {
            JoinButton(viewModel::joinChat,
                modifier = Modifier.align(Alignment.BottomCenter))
        } else {
            BottomRow(textFieldValue, onFieldValueChange = {textFieldValue = it},
                modifier = Modifier.align(Alignment.BottomCenter)) {
                viewModel.sendMessage(textFieldValue)
            }
        }
    }
}