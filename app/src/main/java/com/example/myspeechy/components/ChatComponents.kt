package com.example.myspeechy.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.myspeechy.R
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import java.io.File


@Composable
fun MessagesColumn(
    isPrivate: Boolean,
    userId: String,
    joined: Boolean,
    listState: LazyListState,
    messages: Map<String, Message>,
    filesDir: String,
    onFormatDate: (Long) -> String,
    onEdit: (Map<String, Message>) -> Unit,
    onDelete: (Map<String, Message>) -> Unit,
    onNavigate: (String) -> Unit) {
    val messagesSpacing = dimensionResource(R.dimen.messages_and_chats_spacing)
    var selectedMessageIndex by rememberSaveable { mutableStateOf(-1) }
    val reversedMessages by remember(messages) {
        mutableStateOf(messages.toList().reversed())
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(messagesSpacing, Alignment.Bottom)) {
        if (messages.isNotEmpty()) {
            items(reversedMessages, key = {it.first}) { (messageId, message) ->
                val index = messages.keys.indexOf(messageId)
                //possible chat id between the current and the other user
                val chatId = listOf(message.sender, userId).sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) {it}).joinToString("_")
                Box(contentAlignment = Alignment.Center) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 2.dp,
                                alignment = if (message.sender != userId)
                                    Alignment.Start else Alignment.End)) {
                            if (message.sender != userId && !isPrivate) {
                                ProfilePictureInChat(filesDir = filesDir, sender = message.sender) {
                                    onNavigate(chatId)
                                }
                            }
                            MessageContent(
                                isPrivate = isPrivate,
                                userId = userId,
                                chatId = chatId,
                                index = index,
                                selectedMessageIndex = selectedMessageIndex,
                                message = message,
                                joined = joined,
                                onFormatDate = onFormatDate,
                                onSelectedMessageIndexChange = {selectedMessageIndex = it}
                            ) { onNavigate(chatId) }
                        }
                    if (selectedMessageIndex == index && joined) {
                        DropdownMessageBox(userId = userId,
                            message = Pair(messageId, message),
                            onEdit = onEdit,
                            onDelete = onDelete) { selectedMessageIndex = -1 }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilePictureInChat(filesDir: String,
                         sender: String,
                         onNavigate: () -> Unit) {
    val picSize = dimensionResource(R.dimen.chat_pic_size)
    val picPath = "${filesDir}/profilePics/${sender}/lowQuality/${sender}.jpg"
    if (File(picPath).exists()) {
        var retryHash by remember { mutableStateOf(0) }
        val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
            .data(picPath)
            .setParameter("retry_hash", retryHash)
            .build())
        if (painter.state is AsyncImagePainter.State.Error) {retryHash++}
        Image(painter,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(picSize)
                .clip(CircleShape)
                .clickable { onNavigate() })
    } else {
        Image(painter = painterResource(R.drawable.user),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(picSize)
                .clip(CircleShape)
                .clickable { onNavigate() })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageContent(
    isPrivate: Boolean,
    userId: String,
    chatId: String,
    index: Int,
    selectedMessageIndex: Int,
    message: Message,
    joined: Boolean,
    onFormatDate: (Long) -> String,
    onSelectedMessageIndexChange: (Int) -> Unit,
    onNavigate: (String) -> Unit) {
    val corner = dimensionResource(R.dimen.common_corner_size)
    Box(contentAlignment = if (message.sender == userId) Alignment.CenterEnd else Alignment.CenterEnd) {
        ElevatedCard(
            shape = RoundedCornerShape(corner),
            modifier = Modifier
                .clip(RoundedCornerShape(corner))
                .fillMaxWidth(0.8f)
                .combinedClickable(
                    onClick = { onSelectedMessageIndexChange(-1) },
                    onLongClick = {
                        if (joined && selectedMessageIndex == -1) onSelectedMessageIndexChange(index)
                        else if (selectedMessageIndex != -1) onSelectedMessageIndexChange(-1)
                    }),
            colors = CardDefaults.cardColors(
                containerColor = if (message.sender != userId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.message_content_padding))) {
                val senderUsername = message.senderUsername
                AnimatedVisibility(senderUsername != null){
                    if (senderUsername != null && message.sender != userId && !isPrivate) {
                        Text(senderUsername,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                if (message.sender != userId) {
                                    onNavigate(chatId)
                                }
                            }
                        )
                    }
                }
                Text(
                    message.text,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 5.dp, top = 5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        onFormatDate(message.timestamp),
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    if (message.edited) {
                        Text("Edited",
                            fontSize = 14.sp,
                            modifier = Modifier.alpha(0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMessageBox(
    userId: String,
    message: Pair<String, Message>,
    onEdit: (Map<String, Message>) -> Unit,
    onDelete: (Map<String, Message>) -> Unit,
    onDismiss: () -> Unit
) {
    Box {
        DropdownMenu(expanded = true,
            properties = PopupProperties(focusable = false),
            onDismissRequest = onDismiss) {
            if (message.second.sender == userId) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {onEdit(mapOf(message.first to message.second))
                        onDismiss()})
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {onDelete(mapOf(message.first to message.second))
                        onDismiss()})
            }
        }
    }
}

//This row contains input field and send button
//Currently supports 10 max lines
@Composable
fun BottomRow(textFieldValue: TextFieldValue,
              modifier: Modifier,
              focusRequester: FocusRequester,
              onFieldValueChange: (TextFieldValue) -> Unit,
              onSendButtonClick: () -> Unit) {
    val maxMessageLength = integerResource(R.integer.max_message_length)
    val corner = dimensionResource(R.dimen.common_corner_size)
    Row(modifier = modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
            value = textFieldValue, onValueChange = {
                if (it.text.length <= maxMessageLength) onFieldValueChange(it) },
            shape = RoundedCornerShape(corner),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                unfocusedContainerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester))
        IconButton(
            onClick = { if (textFieldValue.text.isNotEmpty() && textFieldValue.text.isNotBlank()) onSendButtonClick() },
            modifier = Modifier
                .weight(0.2f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                tint = MaterialTheme.colorScheme.surfaceTint,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}


@Composable
fun JoinButton(onClick: () -> Unit, modifier: Modifier) {
    ElevatedButton(onClick = onClick,
        modifier = modifier.fillMaxWidth()) {
        Text("Join")
    }
}

@Composable
fun EditMessageForm(message: Message, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.onTertiaryContainer)) {
            Text(message.senderUsername ?: "", color = MaterialTheme.colorScheme.onPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1)
            Text(message.text, Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClick, Modifier) {
            Icon(imageVector = Icons.Filled.Clear,
                tint = MaterialTheme.colorScheme.onPrimary,
                contentDescription = null)
        }
    }
}
/**
 * @param onCreateOrChange Pair of title and description
 */
@Composable
fun CreateOrChangePublicChatForm(
    chat: Chat? = null,
    onClose: () -> Unit,
    onCreateOrChange: (Pair<String, String>) -> Unit) {
    val placeholders = Pair("Title", "Description")
    val corner = dimensionResource(R.dimen.common_corner_size)
    var title by remember { mutableStateOf(chat?.title ?: "") }
    var description by remember { mutableStateOf(chat?.description ?: "") }
    var isTitleBlank by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(corner))
            .border(
                BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(corner)
            )
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Clear, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(30.dp))
            }
            Text("${if (chat == null) "Create a" else "Change the"} public chat", textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground))
        }
        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            CommonTextField(value = title, isError = isTitleBlank, onChange = { title = it
                                                      isTitleBlank = false}, placeholders = placeholders)
            CommonTextField(value = description, onChange = {description = it}, last = true, placeholders = placeholders)
            OutlinedButton(onClick = {
                if (title.isNotBlank()) {
                    onCreateOrChange(Pair(title, description))
                } else isTitleBlank = true
            },
                Modifier
                    .padding(top = 15.dp)
                    .size(200.dp, 50.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                )) {
                Text(if (chat == null) "Create" else "Change", fontSize = 20.sp)
            }
        }
    }
}

/**
 * @param placeholders "Title" or "Username" goes first
 */
@Composable
fun CommonTextField(value: String,
                    isError: Boolean = false,
                    onChange: (String) -> Unit,
                    last: Boolean=false,
                    placeholders: Pair<String, String>) {
    val focusManager = LocalFocusManager.current
    val corner = dimensionResource(R.dimen.common_corner_size)
    val descriptionMaxChar = integerResource(R.integer.max_description_length)
    val usernameOrTitleMaxChar = integerResource(R.integer.max_username_or_title_length)
    OutlinedTextField(value = value,
        singleLine = !last,
        isError = isError,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(corner),
        keyboardActions = KeyboardActions(
            onNext = {
                if (last) focusManager.clearFocus()
                else focusManager.moveFocus(FocusDirection.Next)
            }
        ),
        onValueChange = {
            if (last && it.length <= descriptionMaxChar) onChange(it) //description
            if (!last && it.length <= usernameOrTitleMaxChar) onChange(it) //username or title
        },
        placeholder = {Text(if (!last) placeholders.first else placeholders.second,
            )},
        supportingText = {Text(if (value.isNotEmpty())
            if (!last) "${value.length} / $usernameOrTitleMaxChar" else "${value.length} / $descriptionMaxChar" else "",
            Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End)},
        textStyle = TextStyle(fontSize = 22.sp),
        modifier = Modifier.fillMaxWidth())
}

@Composable
fun ChatAlertDialog(alertDialogDataClass: AlertDialogDataClass) {
        AlertDialog(
            title = {Text(alertDialogDataClass.title)},
            text = {Text(alertDialogDataClass.text, color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp, textAlign = TextAlign.Center)},
            onDismissRequest = alertDialogDataClass.onDismiss,
            confirmButton = {
                TextButton(onClick = alertDialogDataClass.onConfirm) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = alertDialogDataClass.onDismiss) {
                    Text("Cancel")
                }
            })
}

data class AlertDialogDataClass(val title: String = "",
                                val text: String = "",
                                val onConfirm: () -> Unit = {},
                                val onDismiss: () -> Unit = {})


@Composable
fun ChatPictureComposable(picRef: File) {
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
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

@Composable
fun ScrollDownButton(modifier: Modifier, onClick: () -> Unit) {
    val corner = dimensionResource(R.dimen.common_corner_size)
    IconButton(onClick = onClick,
        modifier = modifier
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.inversePrimary),
                RoundedCornerShape(corner)
            )
            .size(dimensionResource(R.dimen.scroll_down_button_size))
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(0.5f))) {
        Icon(Icons.Filled.KeyboardArrowDown,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null)
    }
}

