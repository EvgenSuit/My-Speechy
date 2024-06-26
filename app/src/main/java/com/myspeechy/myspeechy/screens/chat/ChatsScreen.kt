package com.myspeechy.myspeechy.screens.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.CreateOrChangePublicChatForm
import com.myspeechy.myspeechy.components.CustomAlertDialog
import com.myspeechy.myspeechy.data.chat.Chat
import com.myspeechy.myspeechy.presentation.chat.ChatsViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ChatsScreen(navController: NavHostController,
                    viewModel: ChatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val yourChats = stringResource(R.string.your_chats)
    val allGroups = stringResource(R.string.all_groups)
    var chatScreenPartSelected by rememberSaveable { mutableIntStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }
    var isFormExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val showSearchedChats by remember(uiState.searchedChats) { mutableStateOf(uiState.searchedChats.isNotEmpty()) }
    val usersChatsListState = rememberLazyListState()
    val allChatsListState = rememberLazyListState()
    val firstVisibleChatFromAllChats by remember { derivedStateOf { allChatsListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index } }
    var isAppInBackground by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.startOrStopListening(false)
        viewModel.handleDynamicAllChatsLoading(isAppInBackground, firstVisibleChatFromAllChats)
        isAppInBackground = false
    }
    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        isAppInBackground = true
        viewModel.startOrStopListening(true)
        focusManager.clearFocus(true)
    }
    LaunchedEffect(firstVisibleChatFromAllChats) {
        if (!isAppInBackground) {
            viewModel.handleDynamicAllChatsLoading(false, firstVisibleChatFromAllChats)
        }
    }
    LaunchedEffect(uiState.chatsError) {
        if (uiState.chatsError.isNotEmpty()) {
            Toasty.error(context, uiState.chatsError, Toast.LENGTH_SHORT,true).show()
        }
    }
    Box(
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
                .blur(if (isFormExpanded) 5.dp else 0.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        isFormExpanded = false
                    })
                }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                            .height(dimensionResource(R.dimen.chats_top_row_height))
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    isFormExpanded = false
                                })
                            },
                        verticalAlignment = Alignment.CenterVertically) {
                        ChatSearch(Modifier.onFocusChanged { isFormExpanded = false },
                            onSearch = viewModel::searchForChat,
                            interactionSource = interactionSource,
                            onEmptyTitle = viewModel::clearSearchedChats)
                        IconButton(onClick = {
                            if(!isFormExpanded) navController.navigate("userProfile/${viewModel.userId}") {
                                launchSingleTop = true
                            }
                            isFormExpanded = false },
                            Modifier.fillMaxWidth()) {
                            Icon(
                                Icons.Filled.AccountBox,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    TabRow(selectedTabIndex = chatScreenPartSelected,
                        modifier = Modifier.blur(if (showSearchedChats) 10.dp else 0.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)) {
                        listOf(yourChats, allGroups).forEachIndexed { index, s ->
                            Tab(
                                text = {Text(s, overflow = TextOverflow.Ellipsis)},
                                selected = index == chatScreenPartSelected,
                                onClick = { if (!isFormExpanded) chatScreenPartSelected = index
                                isFormExpanded = false},
                                modifier = Modifier.semantics { contentDescription = s })
                        }
                    }
                    AnimatedContent(targetState = chatScreenPartSelected, label = "") { targetState ->
                        UserChats(
                            listState = if (targetState == 0) usersChatsListState else allChatsListState,
                            chats = if (targetState == 0) uiState.chats else uiState.allPublicChats,
                            modifier = Modifier.blur(if (showSearchedChats) 10.dp else 0.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                            chatScreenPartSelected = targetState,
                            onFormatDate = viewModel::formatDate,
                            onGetChatPic = {chatId ->
                                val otherUserId = chatId?.split("_")?.first { it != viewModel.userId }
                                viewModel.getChatPic(otherUserId ?: "")
                            },
                            onNavigateToChat = {(type, chatId) ->
                                if (!isFormExpanded) navController.navigate("chats/$type/$chatId") {
                                    launchSingleTop = true
                                }
                                isFormExpanded = false},
                            onLeaveChat = { (type, chatId) ->
                                viewModel.leaveChat(type, chatId) })
                    }
                    if (uiState.alertDialogDataClass.title.isNotEmpty()) {
                        CustomAlertDialog(
                            coroutineScope,
                            alertDialogDataClass = uiState.alertDialogDataClass)
                    }
                }
                AnimatedVisibility(chatScreenPartSelected == 1,
                    enter = slideInVertically{it},
                    exit = slideOutVertically{it},
                    modifier = Modifier
                        .align(if (isFormExpanded) Alignment.BottomCenter else Alignment.BottomEnd)
                        .padding(10.dp)) {
                    AnimatedContent(
                        targetState = isFormExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150, 150)) togetherWith
                                    fadeOut(animationSpec = tween(150)) using
                                    SizeTransform { initialSize, targetSize ->
                                        if (targetState) {
                                            keyframes {
                                                IntSize(targetSize.width, initialSize.height) at 300
                                                durationMillis = 300
                                            }
                                        } else {
                                            keyframes {
                                                IntSize(initialSize.width, targetSize.height) at 150
                                                durationMillis = 300
                                            }
                                        }
                                    }
                        }, label = ""
                    ) { targetExpanded ->
                        if (targetExpanded) {
                            CreateOrChangePublicChatForm(
                                modifier = Modifier.align(Alignment.Center),
                                onClose = {isFormExpanded = false}) { (title, description) ->
                                isFormExpanded = false
                                viewModel.createPublicChat(title, description)
                            }
                        } else {
                            FloatingActionButton(onClick = {
                                focusManager.clearFocus(true)
                                isFormExpanded = true
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_public_chat))
                            }
                        }
                    }
                }
        AnimatedVisibility(uiState.searchedChats.isNotEmpty()) {
            SearchedChats(searchedChats = uiState.searchedChats,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        isFormExpanded = false
                        viewModel.clearSearchedChats()
                    })
                },
                onFormatDate = viewModel::formatDate) { searchedChatId ->
                if (!isFormExpanded) {
                    viewModel.clearSearchedChats()
                    focusManager.clearFocus(true)
                    navController.navigate("chats/public/${searchedChatId}") {
                        launchSingleTop = true
                    }
                }
                isFormExpanded = false
            }
        }
    }
}

@Composable
fun ChatSearch(
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    onSearch: (String) -> Unit, onEmptyTitle: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val description = stringResource(R.string.search_for_public_chats)
    val corner = dimensionResource(R.dimen.common_corner_size)
    var chatSearchTitle by remember { mutableStateOf("") }
    LaunchedEffect(chatSearchTitle) {
        if (chatSearchTitle.isNotEmpty() && chatSearchTitle.isNotBlank()) {
            delay(400)
            onSearch(chatSearchTitle)
        } else onEmptyTitle()
    }
    OutlinedTextField(value = chatSearchTitle,
        maxLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(corner),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        placeholder = {Text(description)},
        textStyle = TextStyle(textAlign = TextAlign.Center,
            fontSize = 18.sp),
        suffix = {
            if (chatSearchTitle.isNotEmpty()) IconButton(onClick = {
                chatSearchTitle = ""
                focusManager.clearFocus(true)
            }, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.Clear, contentDescription = null
                )
            }
        },
        onValueChange = {
            chatSearchTitle = it
        },
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth(0.85f)
            .height(70.dp)
            .semantics { contentDescription = description }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchedChats(
    searchedChats: Map<String, Chat>,
    modifier: Modifier,
    onFormatDate: (Long) -> String,
    onGoToChat: (String) -> Unit) {
    val width = dimensionResource(R.dimen.max_width)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.chats_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(top = dimensionResource(id = R.dimen.chat_search_text_field_height))
            .fillMaxSize()
    ) {
        items(searchedChats.entries.toList()) { (chatId, chat) ->
            ElevatedCard(
                onClick = {onGoToChat(chatId)},
                modifier = Modifier
                    .width(width)
                    .shadow(10.dp)
            ) {
                ChatLastMessageContent(currChat = chat, onFormatDate)
                }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserChats(
    listState: LazyListState,
    chats: Map<String, Chat?>,
    chatScreenPartSelected: Int,
    modifier: Modifier,
    onFormatDate: (Long) -> String,
    onGetChatPic: (String?) -> File,
    onNavigateToChat: (Pair<String, String>) -> Unit,
    onLeaveChat: (Pair<String, String>) -> Unit) {
    var selectedChatIndex by remember { mutableIntStateOf(-1) }
    val haptics = LocalHapticFeedback.current
    val width = dimensionResource(R.dimen.max_width)
    LazyColumn(
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.messages_and_chats_spacing)),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 9.dp)
    ) {
        val chatsKeys = chats.keys.toList()
        val chatsValues = chats.values.toList()
        items(chatsValues.size) { i ->
                val currChat = chatsValues[i]
                val chatId = chatsKeys[i]
                Box(contentAlignment = Alignment.Center) {
                        Row(Modifier.width(width),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (currChat != null && currChat.type == "private") {
                                ChatPic(chatId = chatId, onGetChatPic = onGetChatPic)
                            }
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClickLabel = currChat?.title,
                                        onLongClickLabel = "Long click: ${currChat?.title}",
                                        onClick = {
                                            if (currChat != null) {
                                                onNavigateToChat(Pair(currChat.type, chatId))
                                            }
                                        },
                                        onLongClick = {
                                            if (chatScreenPartSelected == 0) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedChatIndex = i
                                            }
                                        }
                                    )
                            ) {
                                ChatLastMessageContent(currChat = currChat, onFormatDate)
                            }
                        }
                    if (selectedChatIndex == i) {
                        LeaveChatBox(
                            onClick = { onLeaveChat(Pair(currChat?.type ?: "public", chatId))
                                selectedChatIndex = -1 },
                            onDismiss = {selectedChatIndex = -1},
                        )
                    }
                }
            }
    }
}

@Composable
fun LeaveChatBox(
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val description = stringResource(R.string.leave_chat)
    Box {
        DropdownMenu(expanded = true,
            properties = PopupProperties(focusable = false),
            onDismissRequest = onDismiss) {
            DropdownMenuItem(text = { Text("Leave chat") },
                onClick = onClick,
                modifier = Modifier.semantics { contentDescription = description })
        }
    }
}

@Composable
fun ChatLastMessageContent(currChat: Chat?,
                           onFormatDate: (Long) -> String) {
    Column(modifier = Modifier
        .height(70.dp)
        .padding(10.dp),
        verticalArrangement = Arrangement.Center) {
        Text(
            currChat?.title ?: "Deleted chat",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row {
            if (currChat != null && currChat.timestamp != 0L) {
                Text(
                    onFormatDate(currChat.timestamp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (currChat != null) {
                Text(
                    currChat.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ChatPic(
    chatId: String,
    onGetChatPic: (String?) -> File) {
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
    val picRef = onGetChatPic(chatId)
    if (picRef.exists()) {
        var retryHash by remember { mutableStateOf(0) }
        val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
            .data(picRef.path)
            .setParameter("retry_hash", retryHash)
            .build())
        if (painter.state is AsyncImagePainter.State.Error) {retryHash++}
        Image(painter,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(chatPicSize)
                .clip(CircleShape)) }
    else {
        Image(
            painter = painterResource(R.drawable.user),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(chatPicSize)
                .clip(CircleShape))
    }
}