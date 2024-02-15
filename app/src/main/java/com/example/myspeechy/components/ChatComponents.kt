package com.example.myspeechy.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.example.myspeechy.data.chat.Message
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun BackButton(onClick: () -> Unit) {
    ElevatedButton(onClick = onClick) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
    }
}

@Composable
fun MessagesColumn(
    userId: String,
    messages: Map<String, Message>,
    onNavigate: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (messages.isNotEmpty()) {
            items(messages.values.toList()) { message ->
                ElevatedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(message.senderUsername,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                    if (message.sender != userId) {
                                        val chatId = listOf(message.sender, userId).sortedWith(
                                            compareBy(String.CASE_INSENSITIVE_ORDER) {it}
                                        ).joinToString("_")
                                        onNavigate(chatId)
                                    }
                                }
                            )
                        Row {
                            Text(
                                SimpleDateFormat("hh:mm:ss").format(Date(message.timestamp)),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.5f)
                            )
                            Spacer(modifier = Modifier.weight(0.1f))
                            Text(
                                message.text,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomRow(textFieldValue: String,
              modifier: Modifier,
              onFieldValueChange: (String) -> Unit,
              onSendButtonClick: () -> Unit) {
    Row(modifier = modifier.fillMaxWidth()) {
        TextField(
            value = textFieldValue, onValueChange = onFieldValueChange,
            modifier = Modifier.weight(0.8f))
        ElevatedButton(
            onClick = { if (textFieldValue.isNotEmpty()) onSendButtonClick() },
            modifier = Modifier.weight(0.2f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                tint = Color.Blue,
                contentDescription = null
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