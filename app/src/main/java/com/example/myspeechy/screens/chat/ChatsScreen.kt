package com.example.myspeechy.screens.chat

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myspeechy.NavScreens
import com.example.myspeechy.R
import com.example.myspeechy.components.ChatAlertDialog
import com.example.myspeechy.components.CommonTextField
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.dataStore
import com.example.myspeechy.screens
import com.example.myspeechy.showNavBarDataStore
import com.example.myspeechy.utils.chat.ChatsViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


@Composable
fun ChatsScreen(
    navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val showNavBar = navController.currentBackStackEntryAsState().value?.destination
        ?.route in screens.map { it.route }
    LaunchedEffect(showNavBar) {
        context.dataStore.edit {navBar ->
            navBar[showNavBarDataStore] = showNavBar
        }
    }
    NavHost(navController = navController, startDestination = NavScreens.ChatsScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(NavScreens.ChatsScreen.route) {
                ChatComposable(navController)
            }
            composable("chats/{type}/{chatId}",
                arguments = listOf(
                    navArgument("type") {type = NavType.StringType},
                    navArgument("chatId") {type = NavType.StringType}),
            ) {backStackEntry ->
                if (backStackEntry.arguments!!.getString("type") == "public") {
                    PublicChatScreen(navController)
                } else {
                    PrivateChatScreen(navController)
                }
            }
            composable("userProfile/{userId}",
                arguments = listOf(navArgument("userId") {type = NavType.StringType})) {
                UserProfileScreen {navController.navigateUp()}
            }
    }
}

@Composable
fun ChatComposable(navController: NavHostController,
                   viewModel: ChatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var chatScreenPartSelected by rememberSaveable { mutableIntStateOf(0) }
    var isFormExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
        viewModel.sortAllPubicChatsOnStartup()
    }
    Box {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onPrimaryContainer)
        ) {
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                ChatSearch(onSearch = viewModel::searchForChat)
                IconButton(onClick = { navController.navigate("userProfile/${viewModel.userId}") },
                    Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Filled.AccountBox,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            TabRow(selectedTabIndex = chatScreenPartSelected) {
                listOf("Your chats", "All groups").forEachIndexed { index, s ->
                    Tab(
                        text = {Text(s, overflow = TextOverflow.Ellipsis)},
                        selected = index == chatScreenPartSelected,
                        onClick = { chatScreenPartSelected = index })
                }
            }
            if (uiState.searchedChat.isNotEmpty()) {
                SearchedChat(searchedChat = uiState.searchedChat) { searchedChatId ->
                    viewModel.onNavigateToSearchedChat()
                    focusManager.clearFocus(true)
                    navController.navigate("chats/public/${searchedChatId}")
                }
            }
                AnimatedContent(targetState = chatScreenPartSelected, label = "") { targetState ->
                        UserChats(
                            chats = if (targetState == 0) uiState.chats else uiState.allPublicChats,
                            chatScreenPartSelected = targetState,
                            onGetChatPic = {chatId ->
                                val otherUserId = chatId?.split("_")?.first { it != viewModel.userId }
                                viewModel.getChatPic(otherUserId ?: "")
                            },
                            onNavigateToChat = {(type, chatId) ->
                                navController.navigate("chats/$type/$chatId")},
                            onLeaveChat = { (type, chatId) ->
                                viewModel.leaveChat(type, chatId)})
                }
            if (uiState.alertDialogDataClass.title.isNotEmpty()) {
                ChatAlertDialog(alertDialogDataClass = uiState.alertDialogDataClass)
            }
        }
        AnimatedVisibility(chatScreenPartSelected == 1,
            enter = slideInVertically{it},
            exit = slideOutVertically{it},
            modifier = Modifier
                .align(Alignment.BottomEnd)
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
                        onClose = {isFormExpanded = false}) { (title, description) ->
                        viewModel.createPublicChat(title, description)
                        isFormExpanded = false
                    }
                } else {
                    FloatingActionButton(onClick = { isFormExpanded = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
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

@Composable
fun ChatSearch(onSearch: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var chatSearchTitle by remember { mutableStateOf("") }
    LaunchedEffect(chatSearchTitle) {
        if (chatSearchTitle.isNotEmpty() && chatSearchTitle.isNotBlank()) {
            delay(400)
            onSearch(chatSearchTitle)
        }
    }
    TextField(value = chatSearchTitle,
        maxLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        suffix = {
            if (chatSearchTitle.isNotEmpty()) IconButton(onClick = {
                chatSearchTitle = ""
                focusManager.clearFocus(true)
            }) {
                Icon(
                    imageVector = Icons.Filled.Clear, contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        onValueChange = {
            chatSearchTitle = it
        }, modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(0.85f)
    )
}

@Composable
fun SearchedChat(
    searchedChat: Map<String, Chat>,
    onGoToChat: (String) -> Unit
) {
    val searchedChatId = searchedChat.keys.first().toString()
    val chat = searchedChat[searchedChatId]
    ElevatedButton(
        onClick = {onGoToChat(searchedChatId)},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 30.dp)
            .shadow(10.dp)
    ) {
        if (chat != null) {
            Column {
                Text(chat.title)
                if (chat.lastMessage.isNotEmpty()) {
                    Row {
                        Text(
                            SimpleDateFormat(
                                "hh:mm:ss"
                            ).format(Date(chat.timestamp)),
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.5f)
                        )
                        Spacer(modifier = Modifier.weight(0.1f))
                        Text(
                            chat.lastMessage,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * @param onCreate Pair of title and description
 */
@Composable
fun CreateOrChangePublicChatForm(
    chat: Chat? = null,
    onClose: () -> Unit,
    onCreate: (Pair<String, String>) -> Unit) {
    val placeholders = Pair("Title", "Description")
    var title by remember { mutableStateOf(chat?.title ?: "") }
    var description by remember { mutableStateOf(chat?.description ?: "") }
    Column(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(2.dp, MaterialTheme.colorScheme.onSecondary),
                shape = RoundedCornerShape(20.dp)
            )
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Clear, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp))
            }
                Text("${if (chat == null) "Create a" else "Change the"} public chat", textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium)

        }
        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp),) {
            CommonTextField(value = title, onChange = { title = it}, placeholders = placeholders)
            CommonTextField(value = description, onChange = {description = it}, last = true, placeholders = placeholders)
            OutlinedButton(onClick = {
               if (title.isNotBlank() && description.isNotBlank()) {
                   onCreate(Pair(title, description))
               }
            },
                Modifier
                    .padding(top = 15.dp)
                    .size(200.dp, 50.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.inversePrimary
                )) {
                Text(if (chat == null) "Create" else "Change", fontSize = 20.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserChats(chats:  Map<String, Chat?>, chatScreenPartSelected: Int,
      onGetChatPic: (String?) -> File,
      onNavigateToChat: (Pair<String, String>) -> Unit,
      onLeaveChat: (Pair<String, String>) -> Unit) {
    var selectedChatIndex by remember { mutableIntStateOf(-1) }
    val haptics = LocalHapticFeedback.current
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp)
    ) {
        val chatsKeys = chats.keys.toList()
        val chatsValues = chats.values.toList()
        items(chatsValues.size) { i ->
                val currChat = chatsValues[i]
                val chatId = chatsKeys[i]
                Box(contentAlignment = Alignment.Center) {
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (currChat != null && currChat.type == "private") {
                                ChatPic(chatId = chatId, onGetChatPic = onGetChatPic)
                            }
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
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
                                ChatLastMessageContent(currChat = currChat)
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
    Box {
        DropdownMenu(expanded = true,
            properties = PopupProperties(focusable = false),
            onDismissRequest = onDismiss) {
            DropdownMenuItem(text = { Text("Leave chat") },
                onClick = onClick)
        }
    }
}

@Composable
fun ChatLastMessageContent(currChat: Chat?) {
    Column(modifier = Modifier
        .height(70.dp)
        .padding(10.dp),
        verticalArrangement = Arrangement.Center) {
        Text(
            currChat?.title ?: "Deleted chat",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            if (currChat != null && currChat.timestamp != 0L) {
                Text(
                    SimpleDateFormat("hh:mm:ss").format(Date(currChat.timestamp)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.5f)
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