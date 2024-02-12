package com.example.myspeechy.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.utils.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatScreen(navController: NavHostController,
               viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    Box {
        Column(
            modifier = Modifier.fillMaxSize()) {
            Row {
                ElevatedButton(onClick = { navController.navigateUp()}) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (!uiState.messages.isNullOrEmpty()) {
                    items(uiState.messages!!.values.toList()) { message ->
                        ElevatedButton(onClick = { /*TODO*/ },
                            modifier = Modifier.fillMaxWidth()) {
                            Column{
                                Text(message.sender)
                                Row {
                                    Text(SimpleDateFormat("hh:mm:ss").format(Date(message.timestamp)),
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(0.5f))
                                    Spacer(modifier = Modifier.weight(0.1f))
                                    Text(message.text,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
            TextField(
                value = textFieldValue, onValueChange = {
                    textFieldValue = it
                },
                modifier = Modifier.weight(0.8f)
            )
            ElevatedButton(
                onClick = { viewModel.sendMessage(textFieldValue) },
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
}
