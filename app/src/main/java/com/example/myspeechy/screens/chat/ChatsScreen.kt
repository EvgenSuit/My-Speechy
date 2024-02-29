package com.example.myspeechy.screens.chat

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.myspeechy.dataStore
import com.example.myspeechy.screens
import com.example.myspeechy.showNavBarDataStore
import com.example.myspeechy.utils.chat.ChatsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
                ChatsColumn(navController)
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsColumn(navController: NavHostController,
                viewModel: ChatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var selectedChatIndex by remember { mutableIntStateOf(-1) }
    val focusManager = LocalFocusManager.current
    var chatSearchTitle by rememberSaveable { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
    LaunchedEffect(chatSearchTitle) {
        delay(400)
        viewModel.searchForChat(chatSearchTitle)
    }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
        viewModel.sortChatsOnStartup()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onPrimaryContainer)
    ) {
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
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
            IconButton(onClick = { navController.navigate("userProfile/${viewModel.userId}") },
                Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.AccountBox,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (uiState.searchedChat.values.isNotEmpty()) {
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
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp)
        ) {
            val chatsKeys = uiState.chats.keys.toList()
            val chatsValues = uiState.chats.values.toList()
            items(chatsValues.size) { i ->
                if (chatsValues[i] != null) {
                    val currChat = chatsValues[i]!!
                    Box(contentAlignment = Alignment.Center) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("chats/${currChat.type}/${chatsKeys[i]}")
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedChatIndex = i
                                    }
                                )
                        ) {
                            Row{
                                if (currChat.type == "private") {
                                    val otherUserId =
                                        chatsKeys[i]?.split("_")?.first { it != viewModel.userId }
                                    val picRef = viewModel.getChatPic(otherUserId ?: "")
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
                            }
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(
                                    currChat.title,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    if (currChat.timestamp != 0L) {
                                        Text(
                                            SimpleDateFormat("hh:mm:ss").format(Date(currChat.timestamp)),
                                            modifier = Modifier.weight(0.5f)
                                        )
                                    }
                                    Text(
                                        currChat.lastMessage,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        if (selectedChatIndex == i) {
                            Box {
                                DropdownMenu(expanded = true,
                                    properties = PopupProperties(focusable = false),
                                    onDismissRequest = {selectedChatIndex = -1}) {
                                    DropdownMenuItem(text = { Text("Delete chat") }, onClick = { })
                                }
                            }
                        }
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