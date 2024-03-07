package com.example.myspeechy.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.ChatPictureComposable
import com.example.myspeechy.components.ChatTopRow
import com.example.myspeechy.components.JoinButton
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.components.ReplyOrEditMessageInfo
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.utils.chat.PublicChatViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PublicChatScreen(navController: NavHostController,
                      viewModel: PublicChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showChangeChatInfoForm by remember { mutableStateOf(false) }
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
    Box(contentAlignment = Alignment.Center) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SideDrawer(
                    isAdmin = uiState.isAdmin,
                    chat = uiState.chat,
                    members = uiState.members, recomposeIds = uiState.picsRecomposeIds, picPaths = uiState.picPaths,
                    onChangeChatInfo = {showChangeChatInfoForm = true},
                    onNavigate = { userId ->
                        if (userId != viewModel.userId) {
                            coroutineScope.launch {
                                drawerState.close()
                                val chatId = listOf(userId, viewModel.userId).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {it})
                                    .joinToString("_")
                                navController.navigate("chats/private/${chatId}")
                            } }

                    })
            }) {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onPrimaryContainer)) {
                ChatTopRow(
                    title = uiState.chat.title,
                    membersSize = if (uiState.chat.timestamp.toInt() != 0) uiState.members.size else null,
                    onSideDrawerShow = { coroutineScope.launch {
                        drawerState.apply { if (isClosed) open() else close() }
                    } },
                    onNavigateUp = navController::navigateUp
                )
                MessagesColumn(viewModel.userId, uiState.joined, listState, uiState.messages,
                    LocalContext.current.cacheDir.path, Modifier.weight(1f),
                    onEdit = {
                        focusManager.clearFocus(true)
                        messageToEdit = it
                        replyMessage = mapOf()
                        val message = it.values.first()
                        textFieldState = TextFieldValue(
                            message.text,
                            selection = TextRange(message.text.length)
                        )
                        focusRequester.requestFocus()
                    },
                    onDelete = { viewModel.deleteMessage(it) },
                    onReply = {
                        focusManager.clearFocus(true)
                        replyMessage = it
                        messageToEdit = mapOf()
                        textFieldState = TextFieldValue()
                        focusRequester.requestFocus()
                    }) { chatId ->
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
                        onFieldValueChange = { textFieldState = it }) {
                        if (messageToEdit.isEmpty()) {
                            viewModel.sendMessage(
                                textFieldState.text,
                                if (replyMessage.keys.isNotEmpty()) replyMessage.keys.first() else ""
                            )
                        } else {
                            viewModel.editMessage(
                                mapOf(
                                    messageToEdit.keys.first() to messageToEdit.values.first()
                                        .copy(text = textFieldState.text)
                                )
                            )
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
        }
        AnimatedVisibility(showChangeChatInfoForm,
            enter = slideInVertically{it},
            exit = slideOutVertically{it}) {
            CreateOrChangePublicChatForm(
                uiState.chat,
                onClose = { showChangeChatInfoForm = false },
                onCreate = {(title, description) ->
                    viewModel.changeChat(title, description)
                    showChangeChatInfoForm = false
                })
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.startOrStopListening(true)
        }
    }
}

@Composable
fun SideDrawer(
    isAdmin: Boolean,
    chat: Chat,
    members: Map<String, String>,
    recomposeIds: Map<String, String>,
    picPaths: Map<String, String>,
    onChangeChatInfo: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth(0.6f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary)
            .padding(top = 15.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ChatInfoColumn(isAdmin, chat, onChangeChatInfo)
        MembersColumn(members = members, recomposeIds = recomposeIds, picPaths = picPaths, onNavigate = onNavigate)
    }
}

@Composable
fun ChatInfoColumn(
    isAdmin: Boolean,
    chat: Chat,
                   onChangeChatInfo: () -> Unit) {
    Column(Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(0.7f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(chat.title)
            if (isAdmin) {
                IconButton(onClick = onChangeChatInfo) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                }
            }
        }
        Text(chat.description)
    }
}

@Composable
fun MembersColumn(members: Map<String, String>,
    recomposeIds: Map<String, String>,
                  picPaths: Map<String, String>,
                  onNavigate: (String) -> Unit) {
    val values by remember(members) {
        mutableStateOf(members.values.toList())
    }
    val keys by remember(members) {
        mutableStateOf(members.keys.toList())
    }
    LazyColumn(

        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items(members.size) {i ->
            ElevatedCard(Modifier.clickable { onNavigate(keys[i]) }) {
                Row(
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .fillMaxWidth()) {
                    val picPath = picPaths[keys[i]]
                    if (picPath != null) {
                        key(recomposeIds[keys[i]]) {
                            ChatPictureComposable(picRef = File(picPath))
                        }
                    }
                    Text(values[i])
                }
            }
        }
    }
}