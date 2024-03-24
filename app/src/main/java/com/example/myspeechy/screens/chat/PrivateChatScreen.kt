package com.example.myspeechy.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.EditMessageForm
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.data.chat.MessagesState
import com.example.myspeechy.presentation.chat.PrivateChatViewModel
import kotlinx.coroutines.launch

@Composable
fun PrivateChatScreen(navController: NavHostController,
                      viewModel: PrivateChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val otherUserExists by remember(uiState.otherUsername) {
        mutableStateOf(uiState.otherUsername != null)
    }
    var textFieldState by remember {
        mutableStateOf(TextFieldValue())
    }
    var messageToEdit by rememberSaveable {
        mutableStateOf(mapOf<String, Message>())
    }
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
    val listState = rememberLazyListState()
    var isAppInBackground by rememberSaveable { mutableStateOf(false) }
    val lastVisibleMessageIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index } }
    val firstVisibleMessage by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull() } }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var retryHash by remember { mutableStateOf(0) }
    val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
        .data(viewModel.picRef.path)
        .setParameter("retry_hash", retryHash)
        .build())
    if (painter.state is AsyncImagePainter.State.Error) { retryHash++ }
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.startOrStopListening(false)
        //listen for the same messages as before if the app was previously in the background
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
        //user is currently at the bottom of the chat, animate to the bottom
        if (!isAppInBackground) {
            viewModel.scrollToBottom(listState, firstVisibleMessage)
        }
    }
    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.onPrimaryContainer)
        .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                BackButton (navController::navigateUp)
                Spacer(modifier = Modifier.weight(0.01f))
                if (viewModel.picRef.exists() && otherUserExists) {
                    Image(painter,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(chatPicSize)
                            .weight(0.15f, fill = false)
                            .clip(CircleShape)
                            .clickable { navController.navigate("userProfile/${viewModel.otherUserId}") })
                } else {
                    Image(painter = painterResource(R.drawable.user),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(chatPicSize)
                            .clip(CircleShape)
                            .clickable { if (otherUserExists) navController.navigate("userProfile/${viewModel.otherUserId}") }
                            .weight(0.15f, fill = false))
                }
                Spacer(modifier = Modifier.weight(0.05f))
                Text(uiState.otherUsername ?: "Deleted account",
                    color = MaterialTheme.colorScheme.onPrimary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.45f)
                )
            }
        if (uiState.messagesState == MessagesState.LOADING) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
            MessagesColumn(viewModel.userId, true, listState, uiState.messages,
                LocalContext.current.filesDir.path, Modifier.weight(1f),
                onFormatDate = viewModel::formatMessageDate,
                onEdit = {
                    if (otherUserExists) {
                        focusManager.clearFocus(true)
                        messageToEdit = it
                        val message = it.values.first()
                        textFieldState = TextFieldValue(message.text,
                            selection = TextRange(message.text.length))
                        focusRequester.requestFocus()
                    } },
                onDelete = { coroutineScope.launch {
                    if(otherUserExists) viewModel.deleteMessage(it)
                } }) {chatId ->
                navController.navigate("chats/private/$chatId")
            }

            if (messageToEdit.isNotEmpty() && uiState.otherUsername != null) {
                EditMessageForm(messageToEdit.values.first()) {
                    messageToEdit = mapOf()
                    textFieldState = TextFieldValue()
                }
            }
            if (uiState.isMemberOfChat != null && uiState.otherUsername != null) {
                BottomRow(textFieldState,
                    focusRequester = focusRequester,
                    modifier = Modifier.height(IntrinsicSize.Min),
                    onFieldValueChange = {textFieldState = it}
                ) {
                    coroutineScope.launch {
                        val text = textFieldState.text
                        if (messageToEdit.isEmpty()) {
                            textFieldState = TextFieldValue()
                            viewModel.sendMessage(text)
                            listState.animateScrollToItem(0)
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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.startOrStopListening(true)
        }
    }
}