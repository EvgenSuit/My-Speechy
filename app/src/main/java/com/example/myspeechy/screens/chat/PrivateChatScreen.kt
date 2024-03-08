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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.components.ReplyOrEditMessageInfo
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.utils.chat.PrivateChatViewModel
import kotlinx.coroutines.launch

@Composable
fun PrivateChatScreen(navController: NavHostController,
                      viewModel: PrivateChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var textFieldState by remember {
        mutableStateOf(TextFieldValue())
    }
    var messageToEdit by rememberSaveable {
        mutableStateOf(mapOf<String, Message>())
    }
    var replyMessage by rememberSaveable {
        mutableStateOf(mapOf<String, Message>())
    }
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var retryHash by remember { mutableStateOf(0) }
    val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
        .data(viewModel.picRef.path)
        .setParameter("retry_hash", retryHash)
        .build())
    if (painter.state is AsyncImagePainter.State.Error) {retryHash++ }
    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.onPrimaryContainer)
        .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
            Row(modifier = Modifier.fillMaxWidth()
                .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                BackButton (navController::navigateUp)
                Spacer(modifier = Modifier.weight(0.01f))
                if (viewModel.picRef.exists()) {
                    Image(painter,
                        contentScale = ContentScale.Inside,
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
                            .clickable { navController.navigate("userProfile/${viewModel.otherUserId}") }
                            .weight(0.15f, fill = false))
                }
                Spacer(modifier = Modifier.weight(0.05f))
                Text(uiState.otherUsername,
                    color = MaterialTheme.colorScheme.onPrimary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 25.sp,
                    modifier = Modifier.weight(0.45f)
                )
            }
        MessagesColumn(viewModel.userId, true, listState, uiState.messages,
                    LocalContext.current.filesDir.path, Modifier.weight(1f),
                    onEdit = {
                        focusManager.clearFocus(true)
                        messageToEdit = it
                        replyMessage = mapOf()
                        val message = it.values.first()
                        textFieldState = TextFieldValue(message.text,
                            selection = TextRange(message.text.length))
                        focusRequester.requestFocus()},
                    onDelete = {
                        viewModel.deleteMessage(it)},
                    onReply = {
                        focusManager.clearFocus(true)
                        replyMessage = it
                        messageToEdit = mapOf()
                        textFieldState = TextFieldValue()
                        focusRequester.requestFocus()}) {chatId ->
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
            BottomRow(textFieldState,
                focusRequester = focusRequester,
                modifier = Modifier.height(IntrinsicSize.Min),
                onFieldValueChange = {textFieldState = it}
            ) {
                if (messageToEdit.isEmpty()) {
                    viewModel.sendMessage(textFieldState.text,
                        if (replyMessage.keys.isNotEmpty()) replyMessage.keys.first() else "")
                } else {
                    val messageToEditValue = messageToEdit.values.first()
                    if (messageToEditValue.text != textFieldState.text) {
                        viewModel.editMessage(mapOf(messageToEdit.keys.first() to messageToEditValue.copy(text = textFieldState.text)))
                        messageToEdit = mapOf()
                    } else return@BottomRow
                }
                textFieldState = TextFieldValue()
                replyMessage = mapOf()
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.startOrStopListening(true)
        }
    }
}