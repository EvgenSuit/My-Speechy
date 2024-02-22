package com.example.myspeechy.components
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspeechy.data.chat.Message
import java.text.SimpleDateFormat
import java.util.Date


@Composable
fun BackButton(
    onClick: () -> Unit) {
    ElevatedButton(onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
    }
}

@Composable
fun MessagesColumn(
    userId: String,
    listState: LazyListState,
    messages: Map<String, Message>,
    modifier: Modifier,
    onNavigate: (String) -> Unit) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.Bottom)) {
        if (messages.isNotEmpty()) {
            items(messages.values.toList().reversed()) { message ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.sender != userId) Arrangement.Start
                        else Arrangement.End) {
                        ElevatedButton(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                AnimatedVisibility(message.senderUsername != null){
                                    Text(message.senderUsername!!,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 18.sp,
                                        modifier = Modifier.clickable {
                                            if (message.sender != userId) {
                                                val chatId = listOf(message.sender, userId).sortedWith(
                                                    compareBy(String.CASE_INSENSITIVE_ORDER) {it})
                                                    .joinToString("_")
                                                onNavigate(chatId)
                                            }
                                        }
                                    )
                                }
                                Text(
                                    message.text,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 5.dp, top = 5.dp))
                                Text(
                                    SimpleDateFormat("hh:mm:ss").format(Date(message.timestamp)),
                                    overflow = TextOverflow.Ellipsis)
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
    Row(modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
            value = textFieldValue, onValueChange = onFieldValueChange,
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                unfocusedContainerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize())
        IconButton(
            onClick = { if (textFieldValue.isNotEmpty()) onSendButtonClick() },
            modifier = Modifier
                .weight(0.4f)

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