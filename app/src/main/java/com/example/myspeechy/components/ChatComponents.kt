package com.example.myspeechy.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import com.example.myspeechy.data.chat.Message
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    ElevatedButton(onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
    }
}

@Composable
fun MessagesColumn(
    userId: String,
    joined: Boolean,
    listState: LazyListState,
    messages: Map<String, Message>,
    filesDir: String,
    modifier: Modifier,
    onEdit: (Map<String, Message>) -> Unit,
    onDelete: (Map<String, Message>) -> Unit,
    onReply: (Map<String, Message>) -> Unit,
    onNavigate: (String) -> Unit) {
    val picSize = dimensionResource(R.dimen.chat_pic_size)
    var selectedMessageIndex by rememberSaveable {
        mutableStateOf(-1)
    }
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.Bottom)) {
        if (messages.isNotEmpty()) {
            itemsIndexed(messages.toList().reversed()) { index, (messageId, message) ->
                val chatId = listOf(message.sender, userId).sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) {it})
                .joinToString("_")
                val repliedMessageIndex = messages.keys.indexOf(message.replyTo)
                Box(contentAlignment = Alignment.Center) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.sender != userId) Arrangement.Start
                        else Arrangement.End) {
                        val showReply = message.replyTo.isNotEmpty() && messages.keys.contains(message.replyTo)
                        if (showReply) {
                            Text("Reply",
                                Modifier.padding(start = 50.dp),
                                color = MaterialTheme.colorScheme.inversePrimary)
                            Spacer(modifier = Modifier.weight(0.01f))
                        }
                        val picPath = "${filesDir}/profilePics/${message.sender}/lowQuality/${message.sender}.jpg"
                        if (message.sender != userId && File(picPath).exists()) {
                            var retryHash by remember { mutableStateOf(0) }
                            val painter = rememberAsyncImagePainter(model = ImageRequest.Builder(LocalContext.current)
                                .data(picPath)
                                .setParameter("retry_hash", retryHash)
                                .build())
                            if (painter.state is AsyncImagePainter.State.Error) {retryHash++}
                            Image(painter,
                                contentScale = ContentScale.Inside,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(picSize)
                                    .clip(CircleShape)
                                    .clickable { onNavigate(chatId) })
                        } else if (!File(picPath).exists() && message.sender != userId) {
                            Image(painter = painterResource(R.drawable.user),
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(picSize)
                                    .clip(CircleShape)
                                    .clickable { onNavigate(chatId) })
                        }
                        Box {
                            ElevatedButton(
                                onClick = { if (joined) selectedMessageIndex = index },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth(if (showReply) 0.9f else 0.8f)
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    val senderUsername = message.senderUsername
                                    AnimatedVisibility(senderUsername != null){
                                        if (senderUsername != null && message.sender != userId) {
                                            Text(senderUsername,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 18.sp,
                                                maxLines = 1,
                                                modifier = Modifier.clickable {
                                                    if (message.sender != userId) {
                                                        onNavigate(chatId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    if (showReply) {
                                        ElevatedButton(onClick = { coroutineScope.launch {listState.animateScrollToItem(repliedMessageIndex)} },
                                            Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(0.7f),
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            )) {
                                            Text(messages.entries.first { it.key == message.replyTo }.value.text,
                                                Modifier.fillMaxWidth(), fontSize = 17.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        message.text,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(bottom = 5.dp, top = 5.dp))
                                    Row{
                                        Text(
                                            SimpleDateFormat("hh:mm:ss").format(Date(message.timestamp)),
                                            overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.weight(1f))
                                        if (message.edited) {
                                            Text("Edited", Modifier.alpha(0.6f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (selectedMessageIndex == index && joined) {
                        Box {
                            DropdownMenu(expanded = true,
                                properties = PopupProperties(focusable = false),
                                onDismissRequest = { selectedMessageIndex = -1}) {
                                if (message.sender == userId) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {onEdit(mapOf(messageId to message))
                                        selectedMessageIndex = -1})
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {onDelete(mapOf(messageId to message))
                                            selectedMessageIndex = -1})
                                }
                                DropdownMenuItem(
                                    text = { Text("Reply") },
                                    onClick = { onReply(mapOf(messageId to message))
                                        selectedMessageIndex = -1})
                            }
                        }
                    }
                }
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
    Row(modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
            value = textFieldValue, onValueChange = { if (it.text.length < 500) onFieldValueChange(it) },
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                unfocusedContainerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .focusRequester(focusRequester))
        IconButton(
            onClick = { if (textFieldValue.text.isNotEmpty() && !textFieldValue.text.all { it == ' ' }) onSendButtonClick() },
            modifier = Modifier
                .weight(0.25f)
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
fun ReplyOrEditMessageInfo(message: Message, onClick: () -> Unit) {
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
 * @param placeholders "Title" or "Username" goes first
 */
@Composable
fun CommonTextField(value: String,
                    onChange: (String) -> Unit,
                    last: Boolean=false,
                    placeholders: Pair<String, String>) {
    val focusManager = LocalFocusManager.current
    val descriptionMaxChar = 70
    val usernameOrTitleMaxChar = 30
    OutlinedTextField(value = value,
        singleLine = !last,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (last) focusManager.clearFocus()
                else focusManager.moveFocus(FocusDirection.Next)
            }
        ),
        onValueChange = {
            if (last && it.length < descriptionMaxChar) onChange(it) //description
            if (!last && it.length < usernameOrTitleMaxChar) onChange(it) //username or title
        },
        placeholder = {Text(if (!last) placeholders.first else placeholders.second)},
        supportingText = {Text(if (!last) "${value.length} / $usernameOrTitleMaxChar" else "${value.length} / $descriptionMaxChar",
            Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.End)},
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
            focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSecondary
        ),
        textStyle = TextStyle(fontSize = 22.sp),
        modifier = Modifier.fillMaxWidth())
}

@Composable
fun ChatAlertDialog(alertDialogDataClass: AlertDialogDataClass) {
        AlertDialog(
            title = {Text(alertDialogDataClass.title, textAlign = TextAlign.Center)},
            text = {Text(alertDialogDataClass.text)},
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
fun PublicChatTopRow(
    title: String,
    membersSize: Int?,
    onSideDrawerShow: () -> Unit,
    onNavigateUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BackButton(onNavigateUp)
        Spacer(modifier = Modifier.weight(0.1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)) {
            Text(title,
                color = MaterialTheme.colorScheme.onPrimary,
                overflow = TextOverflow.Ellipsis,
                fontSize = 25.sp,
                maxLines = 1)
            if (membersSize != null) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(membersSize.toString())
                    Icon(Icons.Filled.Person, contentDescription = null)
                }
            }
        }
        IconButton(onClick = onSideDrawerShow, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Menu, contentDescription = null,
                modifier = Modifier.size(50.dp))
        }
    }
}

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