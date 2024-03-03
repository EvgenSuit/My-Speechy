package com.example.myspeechy.screens.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.JoinButton
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.components.ReplyOrEditMessageInfo
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.utils.chat.PublicChatViewModel
import kotlinx.coroutines.launch

@Composable
fun PublicChatScreen(navController: NavHostController,
                      viewModel: PublicChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var textFieldState by remember {
        mutableStateOf(TextFieldValue())
    }
    var messageToEdit by rememberSaveable {
        mutableStateOf(mapOf<String, Message>())
    }
    var replyMessage by rememberSaveable {
        mutableStateOf(mapOf<String, Message>())
    }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.onPrimaryContainer)) {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                BackButton(navController::navigateUp)
                Text(uiState.chat.title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 50.dp))
            }
            MessagesColumn(viewModel.userId, uiState.joined, listState, uiState.messages,
                    LocalContext.current.cacheDir.path, Modifier.weight(1f),
                    onEdit = {focusManager.clearFocus(true)
                        messageToEdit = it
                        replyMessage = mapOf()
                        val message = it.values.first()
                        textFieldState = TextFieldValue(message.text,
                            selection = TextRange(message.text.length)
                        )
                        focusRequester.requestFocus()},
                    onDelete = {viewModel.deleteMessage(it)},
                    onReply = {focusManager.clearFocus(true)
                        replyMessage = it
                        Log.d("REPLY MESSAGE", replyMessage.toString())
                        messageToEdit = mapOf()
                        textFieldState = TextFieldValue()
                        focusRequester.requestFocus()}){ chatId ->
                    navController.navigate("chats/private/$chatId")
                }
        if (replyMessage.isNotEmpty()) {
            ReplyOrEditMessageInfo(replyMessage.values.first()) {
                replyMessage = mapOf()
                messageToEdit = mapOf()
                textFieldState = TextFieldValue()
            }
        } else if (messageToEdit.isNotEmpty()) {
            ReplyOrEditMessageInfo(messageToEdit.values.first()) {
                replyMessage = mapOf()
                messageToEdit = mapOf()
                textFieldState = TextFieldValue()
            }
        }
            if (!uiState.joined) {
                JoinButton(viewModel::joinChat, Modifier)
            } else {
                BottomRow(textFieldState,
                    focusRequester = focusRequester,
                    modifier = Modifier.height(IntrinsicSize.Min),
                    onFieldValueChange = {textFieldState = it}) {
                    if (messageToEdit.isEmpty()) {
                        viewModel.sendMessage(textFieldState.text,
                            if (replyMessage.keys.isNotEmpty()) replyMessage.keys.first() else "")
                    } else {
                        viewModel.editMessage(mapOf(messageToEdit.keys.first() to messageToEdit.values.first().copy(text = textFieldState.text)))
                        messageToEdit = mapOf()
                    }
                    textFieldState = TextFieldValue()
                    replyMessage = mapOf()
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