package com.example.myspeechy.screens.chat

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.ChatAlertDialog
import com.example.myspeechy.components.ChatPictureComposable
import com.example.myspeechy.components.CreateOrChangePublicChatForm
import com.example.myspeechy.components.EditMessageForm
import com.example.myspeechy.components.JoinButton
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.components.ScrollDownButton
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.data.chat.MessagesState
import com.example.myspeechy.presentation.chat.PublicChatViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PublicChatScreen(navController: NavHostController,
                      viewModel: PublicChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val membersListState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val firstVisibleMessage by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull() } }
    val lastVisibleMessageIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index } }
    var isAppInBackground by rememberSaveable { mutableStateOf(false) }
    val showScrollDownButton by remember {
        derivedStateOf { uiState.messages.isNotEmpty() && firstVisibleMessage?.index != 0
                && listState.canScrollBackward && !listState.isScrollInProgress } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.startOrStopListening(false)
        //listen for the same messages and members as before if the app was previously in the background
        viewModel.handleDynamicMessageLoading(isAppInBackground, lastVisibleMessageIndex)
        isAppInBackground = false
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        isAppInBackground = true
        viewModel.startOrStopListening(true)
        focusManager.clearFocus(true)
    }
    LaunchedEffect(lastVisibleMessageIndex) {
        if (!isAppInBackground) {
            viewModel.handleDynamicMessageLoading(false, lastVisibleMessageIndex)
        }
    }
    LaunchedEffect(uiState.messages.entries.lastOrNull()) {
        //if a new message was just sent and the
        //user was at the bottom of the chat, animate to the bottom
        if (!isAppInBackground) {
            viewModel.scrollToBottom(listState, firstVisibleMessage)
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            Toasty.error(context, uiState.errorMessage, Toasty.LENGTH_SHORT, true)
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showChangeChatInfoForm by remember(drawerState.isClosed) { mutableStateOf(false) }
    var textFieldState by remember {
        mutableStateOf(TextFieldValue())
    }
    var messageToEdit by rememberSaveable {
        mutableStateOf(mapOf<String, Message>())
    }
    Box(contentAlignment = Alignment.Center) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SideDrawer(
                    admin = uiState.admin,
                    isAdmin = uiState.isAdmin,
                    chat = uiState.chat,
                    joined = uiState.joined,
                    members = uiState.members, recomposeIds = uiState.picsRecomposeIds, picPaths = uiState.picPaths,
                    membersListState = membersListState,
                    onLeave = viewModel::leaveChat,
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
                .background(MaterialTheme.colorScheme.background)) {
                PublicChatTopRow(
                    title = if (uiState.chat.title.isEmpty() && uiState.chatLoaded) "Deleted chat" else uiState.chat.title,
                    membersSize = if (uiState.chat.timestamp.toInt() != 0) uiState.members.size else null,
                    onSideDrawerShow = { coroutineScope.launch {
                        drawerState.apply { if (isClosed) open() else close() }
                    } },
                    onNavigateUp = navController::navigateUp
                )
                if (uiState.messagesState == MessagesState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Box(
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally), contentAlignment = Alignment.BottomCenter) {
                    if (uiState.messagesState != MessagesState.EMPTY) {
                        MessagesColumn(
                            false,
                            viewModel.userId,
                            uiState.joined,
                            listState,
                            uiState.messages,
                            LocalContext.current.cacheDir.path,
                            onFormatDate = viewModel::formatMessageDate,
                            onEdit = {
                                focusManager.clearFocus(true)
                                messageToEdit = it
                                val message = it.values.first()
                                textFieldState = TextFieldValue(
                                    message.text,
                                    selection = TextRange(message.text.length)
                                )
                                focusRequester.requestFocus()
                            },
                            onDelete = {
                                coroutineScope.launch { viewModel.deleteMessage(it) }
                            }) { chatId ->
                            navController.navigate("chats/private/$chatId")
                        }
                        if (showScrollDownButton) {
                            ScrollDownButton(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(25.dp)) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        }
                    } else {
                        Text("No messages here yet",
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center))
                    }
                }
                if (messageToEdit.isNotEmpty()) {
                    EditMessageForm(messageToEdit.values.first()) {
                        messageToEdit = mapOf()
                        textFieldState = TextFieldValue()
                    }
                }
                if (uiState.chat.title.isNotEmpty()) {
                    if (uiState.chatLoaded && !uiState.joined) {
                        JoinButton(viewModel::joinChat, Modifier)
                    }
                    else if (uiState.chatLoaded) {
                        BottomRow(textFieldState,
                            focusRequester = focusRequester,
                            modifier = Modifier.heightIn(max = 200.dp),
                            onFieldValueChange = { textFieldState = it }) {
                            coroutineScope.launch {
                                if (messageToEdit.isEmpty()) {
                                    viewModel.sendMessage(textFieldState.text)
                                } else {
                                    viewModel.editMessage(
                                        mapOf(
                                            messageToEdit.keys.first() to messageToEdit.values.first()
                                                .copy(text = textFieldState.text)
                                        )
                                    )
                                }
                                textFieldState = TextFieldValue()
                                messageToEdit = mapOf()
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(showChangeChatInfoForm,
            enter = slideInHorizontally{-it},
            exit = slideOutHorizontally{-it}) {
            CreateOrChangePublicChatForm(
                uiState.chat,
                onClose = { showChangeChatInfoForm = false },
                onCreateOrChange = { (title, description) ->
                    coroutineScope.launch {
                        viewModel.changeChat(title, description)
                        showChangeChatInfoForm = false
                    }
                })
        }
    }
    if (uiState.joined && uiState.alertDialogDataClass.title.isNotEmpty()) {
        ChatAlertDialog(uiState.alertDialogDataClass)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.startOrStopListening(true)
        }
    }
}

@Composable
fun SideDrawer(
    admin: String?,
    isAdmin: Boolean,
    joined: Boolean,
    chat: Chat,
    members: Map<String, String>,
    recomposeIds: Map<String, String>,
    picPaths: Map<String, String>,
    membersListState: LazyListState,
    onChangeChatInfo: () -> Unit,
    onLeave: () -> Unit,
    onNavigate: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.7f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (chat.title.isNotEmpty()) {
            ChatInfoColumn(isAdmin, chat, onChangeChatInfo)
        }
        if (joined) {
            IconButton(onClick = onLeave) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.back_button_size)))
            }
        }
        MembersColumn(admin = admin, members = members, recomposeIds = recomposeIds, picPaths = picPaths,
            listState = membersListState,
            onNavigate = onNavigate)
    }
}

@Composable
fun ChatInfoColumn(
    isAdmin: Boolean,
    chat: Chat,
    onChangeChatInfo: () -> Unit) {
    val corner = dimensionResource(R.dimen.common_corner_size)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(1f))
            .clickable { if (isAdmin) onChangeChatInfo() }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (chat.description.isNotEmpty()) Arrangement.spacedBy(10.dp) else Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(chat.title,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                fontSize = 25.sp)
            if (isAdmin) Icon(Icons.Filled.Edit,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = null)
        }
        Text(chat.description,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center)
    }
}

@Composable
fun MembersColumn(
    admin: String?,
    members: Map<String, String>,
    recomposeIds: Map<String, String>,
    picPaths: Map<String, String>,
    listState: LazyListState,
    onNavigate: (String) -> Unit) {
    val usernames by remember(members) {
        mutableStateOf(members.values.toList())
    }
    val userIds by remember(members) {
        mutableStateOf(members.keys.toList())
    }
    LazyColumn(
        state = listState) {
        items(members.size) {i ->
            ElevatedCard(
                Modifier
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.common_corner_size)))
                    .padding(2.dp)
                    .clickable { if (usernames[i] != "Deleted user") onNavigate(userIds[i]) }) {
                Row(
                    Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .fillMaxSize()
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start) {
                    val userId = userIds[i]
                    val picPath = picPaths[userId]
                    key(recomposeIds[userId]) {
                        ChatPictureComposable(picRef = File(picPath ?: ""))
                    }
                    Column(Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(usernames[i], fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (userId == admin) {
                            Text("Admin", color = MaterialTheme.colorScheme.surfaceTint)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PublicChatTopRow(
    title: String,
    membersSize: Int?,
    onSideDrawerShow: () -> Unit,
    onNavigateUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BackButton(onNavigateUp)
        Spacer(modifier = Modifier.weight(0.01f))
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)) {
            Text(title,
                color = MaterialTheme.colorScheme.primary,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1)
            if (membersSize != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)) {
                    Text(membersSize.toString(),
                        color = MaterialTheme.colorScheme.onBackground)
                    Icon(Icons.Filled.Person,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null)
                }
            }
        }
        IconButton(onClick = onSideDrawerShow, modifier = Modifier.weight(0.2f)) {
            Icon(Icons.Filled.Menu,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = null,
                modifier = Modifier.size(50.dp))
        }
    }
}