package com.myspeechy.myspeechy.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.myspeechy.myspeechy.R
import com.myspeechy.myspeechy.components.BackButton
import com.myspeechy.myspeechy.components.BottomRow
import com.myspeechy.myspeechy.components.EditMessageForm
import com.myspeechy.myspeechy.components.MessagesColumn
import com.myspeechy.myspeechy.components.ScrollDownButton
import com.myspeechy.myspeechy.data.chat.Message
import com.myspeechy.myspeechy.data.chat.MessagesState
import com.myspeechy.myspeechy.presentation.chat.PrivateChatViewModel
import kotlinx.coroutines.delay
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
    val width = dimensionResource(R.dimen.max_width)
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
    val listState = rememberLazyListState()
    var isAppInBackground by rememberSaveable { mutableStateOf(false) }
    val lastVisibleMessageIndex by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index } }
    val firstVisibleMessage by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull() } }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val canScroll by remember{ derivedStateOf {
        uiState.messages.isNotEmpty() &&
        !listState.isScrollInProgress && listState.canScrollBackward && firstVisibleMessage?.index != 0} }
    var showScrollDownButton by remember {
        mutableStateOf(false)
    }
    val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
        .data(viewModel.picRef.path)
        .build())
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.startOrStopListening(false)
        //listen for the same messages as before if the app was previously in the background
        viewModel.handleDynamicMessageLoading(isAppInBackground, lastVisibleMessageIndex)
        isAppInBackground = false
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        isAppInBackground = true
        focusManager.clearFocus(true)
        viewModel.startOrStopListening(true)
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
    LaunchedEffect(canScroll) {
        if (canScroll) delay(300)
        showScrollDownButton = canScroll
    }
   BoxWithConstraints(
       contentAlignment = Alignment.Center
   ) {
       Column(modifier = Modifier
           .background(MaterialTheme.colorScheme.background)
           .fillMaxHeight()
           .let { if (maxWidth < width) it.fillMaxWidth() else it.width(width) },
           verticalArrangement = Arrangement.Center
       ) {
           Row(modifier = Modifier
               .fillMaxWidth()
               .pointerInput(Unit) {
                   detectTapGestures {
                       if (uiState.otherUsername != null) navController.navigate("userProfile/${viewModel.otherUserId}")
                   }
               }
               .padding(10.dp),
               verticalAlignment = Alignment.CenterVertically,
               horizontalArrangement = Arrangement.SpaceEvenly) {
               BackButton (navController::navigateUp)
               if (viewModel.picRef.exists() && otherUserExists) {
                   Image(painter,
                       contentScale = ContentScale.Crop,
                       contentDescription = null,
                       modifier = Modifier
                           .size(chatPicSize)
                           .clip(CircleShape))
               } else {
                   Image(painter = painterResource(R.drawable.user),
                       contentScale = ContentScale.Crop,
                       contentDescription = null,
                       modifier = Modifier
                           .size(chatPicSize)
                           .clip(CircleShape))
               }
               Box(modifier = Modifier.fillMaxWidth(),
                   contentAlignment = Alignment.Center) {
                   Text(uiState.otherUsername ?: "Deleted account",
                       color = MaterialTheme.colorScheme.primary,
                       overflow = TextOverflow.Ellipsis,
                       maxLines = 1,
                       style = MaterialTheme.typography.bodyMedium,
                   )
               }
           }
           if (uiState.messagesState == MessagesState.LOADING || !uiState.chatLoaded) {
               Box(contentAlignment = if (!uiState.chatLoaded) Alignment.Center else Alignment.TopCenter,
                   modifier = Modifier.align(Alignment.CenterHorizontally).apply { if (!uiState.chatLoaded) fillMaxHeight() }) {
                   CircularProgressIndicator()
               }
           }
           Box(contentAlignment = Alignment.BottomCenter,
               modifier = Modifier
                   .weight(1f)
                   .align(Alignment.CenterHorizontally)) {
               if (uiState.messagesState != MessagesState.EMPTY && viewModel.userId != null) {
                   MessagesColumn(
                       true,
                       viewModel.userId,
                       true,
                       listState,
                       uiState.messages,
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
                   this@Column.AnimatedVisibility(showScrollDownButton,
                       enter = slideInVertically(animationSpec = tween(100)) {it},
                       exit = slideOutVertically(animationSpec = tween(100)) {it},
                       modifier = Modifier
                           .align(Alignment.BottomEnd)
                           .padding(20.dp)) {
                       ScrollDownButton {
                           coroutineScope.launch {
                               listState.animateScrollToItem(0)
                           }
                       }
                   }
               } else {
                   Text("No messages here yet",
                       color = MaterialTheme.colorScheme.onBackground,
                       textAlign = TextAlign.Center,
                       modifier = Modifier.align(Alignment.Center))
               }
           }

           if (messageToEdit.isNotEmpty() && uiState.otherUsername != null) {
               EditMessageForm(messageToEdit.values.first()) {
                   messageToEdit = mapOf()
                   textFieldState = TextFieldValue()
               }
           }
           AnimatedVisibility(uiState.isMemberOfChat != null && uiState.otherUsername != null) {
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
   }
}