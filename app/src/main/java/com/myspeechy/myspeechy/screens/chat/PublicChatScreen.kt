package com.myspeechy.myspeechy.screens.chat

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.BackButton
import com.myspeechy.myspeechy.components.BottomRow
import com.myspeechy.myspeechy.components.ChatPictureComposable
import com.myspeechy.myspeechy.components.CreateOrChangePublicChatForm
import com.myspeechy.myspeechy.components.CustomAlertDialog
import com.myspeechy.myspeechy.components.EditMessageForm
import com.myspeechy.myspeechy.components.JoinButton
import com.myspeechy.myspeechy.components.MessagesColumn
import com.myspeechy.myspeechy.components.ScrollDownButton
import com.myspeechy.myspeechy.data.chat.Chat
import com.myspeechy.myspeechy.data.chat.MembersState
import com.myspeechy.myspeechy.data.chat.Message
import com.myspeechy.myspeechy.data.chat.MessagesState
import com.myspeechy.myspeechy.presentation.chat.PublicChatViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PublicChatScreen(navController: NavHostController,
                      viewModel: PublicChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val messagesListState = rememberLazyListState()
    val membersListState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val formWidth = dimensionResource(R.dimen.max_create_or_change_chat_form_width)
    val firstVisibleMessage by remember(messagesListState) { derivedStateOf { messagesListState.layoutInfo.visibleItemsInfo.firstOrNull() } }
    val lastVisibleMemberIndex by remember(messagesListState) { derivedStateOf { membersListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index } }
    val lastVisibleMessageIndex by remember(messagesListState) { derivedStateOf { messagesListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index } }
    var isAppInBackground by rememberSaveable { mutableStateOf(false) }
    val canScroll by remember {
        derivedStateOf { uiState.messages.isNotEmpty() && firstVisibleMessage?.index != 0
                && messagesListState.canScrollBackward && !messagesListState.isScrollInProgress } }
    var showScrollDownButton by rememberSaveable {
        mutableStateOf(false)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.startOrStopListening(false)
        //listen for the same messages and members as before if the app was previously in the background
        viewModel.handleDynamicMembersLoading(isAppInBackground, lastVisibleMemberIndex)
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
    LaunchedEffect(lastVisibleMemberIndex) {
        if (!isAppInBackground) {
            viewModel.handleDynamicMembersLoading(false, lastVisibleMemberIndex)
        }
    }
    LaunchedEffect(uiState.messages.entries.lastOrNull()) {
        //if a new message was just sent and the
        //user was at the bottom of the chat, animate to the bottom
        if (!isAppInBackground) {
            viewModel.scrollToBottom(messagesListState, firstVisibleMessage)
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty() && uiState.chat.title.isNotEmpty()) {
            Toasty.error(context, uiState.errorMessage, Toasty.LENGTH_SHORT, true).show()
        }
    }
    LaunchedEffect(canScroll) {
        if (canScroll) delay(300)
        showScrollDownButton = canScroll
    }
    var showChangeChatInfoForm by remember(drawerState.isClosed) { mutableStateOf(false) }
    var textFieldState by remember {
        mutableStateOf(TextFieldValue())
    }
    var messageToEdit by rememberSaveable {
        mutableStateOf(mapOf<String, Message>())
    }
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val maxWidth = maxWidth
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    SideDrawer(
                        state = uiState.membersState,
                        admin = uiState.admin,
                        isAdmin = uiState.isAdmin,
                        chat = uiState.chat,
                        joined = uiState.joined,
                        members = uiState.members, recomposeIds = uiState.picsRecomposeIds, picPaths = uiState.picPaths,
                        membersListState = membersListState,
                        maxWidth = maxWidth,
                        onLeave = {viewModel.leaveChat(uiState.isAdmin)},
                        onChangeChatInfo = {showChangeChatInfoForm = true},
                        onNavigate = { userId ->
                            if (userId != viewModel.userId) {
                                coroutineScope.launch {
                                    drawerState.close()
                                    val chatId = listOf(userId, viewModel.userId).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {it})
                                        .joinToString("_")
                                    navController.navigate("chats/private/${chatId}") {
                                        launchSingleTop = true
                                    }
                                } }
                        })
                }) {
                Box(Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    PublicChatTopRow(
                        title = if (uiState.chat.title.isEmpty() && uiState.chatLoaded) "Deleted chat" else uiState.chat.title,
                        membersSize = uiState.memberCount,
                        isChatNull = uiState.chat.title.isEmpty(),
                        onSideDrawerShow = { coroutineScope.launch {
                            drawerState.apply { if (isClosed) open() else close() }
                        } },
                        onNavigateUp = navController::navigateUp
                    )
                    if (uiState.messagesState == MessagesState.LOADING || !uiState.chatLoaded) {
                        Box(contentAlignment = if (!uiState.chatLoaded) Alignment.Center else Alignment.TopCenter,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .apply { if (!uiState.chatLoaded) fillMaxHeight() }) {
                            CircularProgressIndicator()
                        }
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
                                messagesListState,
                                uiState.messages,
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
                                navController.navigate("chats/private/$chatId") {
                                    launchSingleTop = true //when set to true, when a user navigates back from that private chat,
                                    //the main chats page gets displayed instead of a chat they've just been to
                                }
                            }
                            this@Column.AnimatedVisibility(showScrollDownButton,
                                enter = slideInVertically {it},
                                exit = slideOutVertically {it},
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(20.dp)) {
                                ScrollDownButton {
                                    coroutineScope.launch {
                                        messagesListState.animateScrollToItem(0)
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
                    AnimatedVisibility(uiState.chat.title.isNotEmpty(),
                        enter = slideInVertically { it }) {
                        if (uiState.chatLoaded && !uiState.joined) {
                            JoinButton({ coroutineScope.launch { viewModel.joinChat() }
                            }, Modifier)
                        } else if (uiState.chatLoaded) {
                            BottomRow(textFieldState,
                                focusRequester = focusRequester,
                                modifier = Modifier.heightIn(max = 200.dp),
                                onFieldValueChange = { textFieldState = it }) {
                                coroutineScope.launch {
                                    val text = textFieldState.text
                                    if (messageToEdit.isEmpty()) {
                                        textFieldState = TextFieldValue()
                                        viewModel.sendMessage(text)
                                        messagesListState.animateScrollToItem(0)
                                    } else {
                                        val messageToEditValue = messageToEdit.values.first()
                                        val messageToEditKey = messageToEdit.keys.first()
                                        if (messageToEditValue.text != text) {
                                            textFieldState = TextFieldValue()
                                            messageToEdit = mapOf()
                                            viewModel.editMessage(mapOf(messageToEditKey to messageToEditValue.copy(text = text)))
                                        }
                                    }
                                }
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
                modifier = Modifier.let { if (maxWidth < formWidth) it.fillMaxWidth() else it.width(formWidth) },
                onClose = { showChangeChatInfoForm = false },
                onCreateOrChange = { (title, description) ->
                    if (uiState.chat.title != title || uiState.chat.description != description) {
                        coroutineScope.launch {
                            viewModel.changeChat(title, description)
                            showChangeChatInfoForm = false
                        }
                    } else showChangeChatInfoForm = false
                })
        }
    }

    if (uiState.joined && uiState.alertDialogDataClass.title.isNotEmpty()) {
        CustomAlertDialog(
            coroutineScope,
            uiState.alertDialogDataClass)
    }
}

@Composable
fun SideDrawer(
    state: MembersState,
    admin: String?,
    isAdmin: Boolean,
    joined: Boolean,
    chat: Chat,
    members: Map<String, String?>,
    recomposeIds: Map<String, String>,
    picPaths: Map<String, String>,
    membersListState: LazyListState,
    maxWidth: Dp,
    onChangeChatInfo: () -> Unit,
    onLeave: suspend () -> Unit,
    onNavigate: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val dimenMaxWidth = dimensionResource(R.dimen.max_width)
    Column(
        Modifier
            .fillMaxWidth(if (maxWidth < dimenMaxWidth) 0.7f else 0.4f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (chat.title.isNotEmpty()) {
            ChatInfoColumn(isAdmin, chat, onChangeChatInfo)
        }
        if (joined) {
            IconButton(onClick = {
                coroutineScope.launch {
                    onLeave()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.back_button_size)))
            }
        }
        MembersColumn(
            state = state,
            admin = admin, members = members, recomposeIds = recomposeIds, picPaths = picPaths,
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
    state: MembersState,
    admin: String?,
    members: Map<String, String?>,
    recomposeIds: Map<String, String>,
    picPaths: Map<String, String>,
    listState: LazyListState,
    onNavigate: (String) -> Unit) {
    val usernames by remember(members) {
        derivedStateOf { members.values.toList() }
    }
    val userIds by remember(members) {
        derivedStateOf { members.keys.toList() }
    }
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.chats_padding)),
        state = listState) {
        items(userIds, key = {it}) {userId ->
            val username = usernames[userIds.indexOf(userId)]
            ElevatedCard(
                Modifier
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.common_corner_size)))
                    .padding(2.dp)
                    .clickable { if (username != null) onNavigate(userId) }) {
                Row(
                    Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .fillMaxSize()
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start) {
                    val picPath = picPaths[userId]
                    key(recomposeIds[userId]) {
                        ChatPictureComposable(picRef = File(picPath ?: ""))
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(username ?: stringResource(R.string.deleted_account),
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        if (userId == admin) {
                            Text("Admin", color = MaterialTheme.colorScheme.surfaceTint)
                        }
                    }
                }
            }
        }
        if (state == MembersState.LOADING)
            item {
                LinearProgressIndicator(modifier = Modifier)
            }
    }
}
@Composable
fun PublicChatTopRow(
    title: String,
    membersSize: Int?,
    isChatNull: Boolean,
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)) {
            Text(title,
                color = MaterialTheme.colorScheme.primary,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1)
            if (membersSize != null && !isChatNull) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)) {
                    Text(membersSize.toString(),
                        color = MaterialTheme.colorScheme.onBackground)
                    Icon(Icons.Filled.Person,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null)
                }
            }
        }
        if (!isChatNull) {
            IconButton(onClick = onSideDrawerShow, modifier = Modifier.weight(0.2f)) {
                Icon(Icons.Filled.Menu,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp))
            }
        }
    }
}