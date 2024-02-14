package com.example.myspeechy.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.utils.ChatsViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsScreen(navController: NavHostController,
                viewModel: ChatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember {
        FocusRequester()
    }
    val focusManager = LocalFocusManager.current
    var chatSearchTitle by rememberSaveable {
        mutableStateOf("")
    }
    LaunchedEffect(chatSearchTitle) {
        delay(400)
        viewModel.searchForChat(chatSearchTitle)
    }
    Column {
        TextField(value = chatSearchTitle,
            maxLines = 1,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {focusManager.clearFocus()}
            ),
            suffix = {if (chatSearchTitle.isNotEmpty())  IconButton(onClick = { chatSearchTitle = "" }) {
                Icon(imageVector = Icons.Filled.Clear, contentDescription = null,
                    modifier = Modifier.size(30.dp))
            }},
            onValueChange = {
            chatSearchTitle = it
        }, modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        if (!uiState.searchedChat.values.isNullOrEmpty()) {
            val searchedChatId = uiState.searchedChat.keys.first().toString()
            val searchedChat = uiState.searchedChat[searchedChatId]
            ElevatedButton(
                onClick = {
                    chatSearchTitle = ""
                    navController.navigate("chats/public/${searchedChatId}")
                          },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp)
                    .shadow(10.dp)
            ) {
                Column {
                    Text(searchedChat!!.title)
                    if (searchedChat.lastMessage.isNotEmpty()) {
                        Row {
                            Text(
                                SimpleDateFormat(
                                    "hh:mm:ss"
                                ).format(Date(searchedChat.timestamp)),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.5f)
                            )
                            Spacer(modifier = Modifier.weight(0.1f))
                            Text(
                                searchedChat.lastMessage,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                    }
                }
            }
        }
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            val chatsKeys = uiState.chats.keys.toList()
            val chatsValues = uiState.chats.values.toList()
            items(chatsValues.size) { i ->
                if (chatsValues[i] != null) {
                    ElevatedButton(
                        onClick = { navController.navigate("chats/${chatsValues[i]!!.type}/${chatsKeys[i]}") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                chatsValues[i]!!.title,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    SimpleDateFormat("hh:mm:ss").format(Date(chatsValues[i]!!.timestamp)),
                                    modifier = Modifier.weight(0.5f)
                                )
                                Spacer(modifier = Modifier.weight(0.1f))
                                Text(
                                    chatsValues[i]!!.lastMessage,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}