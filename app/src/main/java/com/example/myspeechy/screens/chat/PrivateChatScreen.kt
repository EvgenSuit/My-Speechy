package com.example.myspeechy.screens.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.utils.chat.PrivateChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PrivateChatScreen(navController: NavHostController,
                      viewModel: PrivateChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var textFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
    val decodedPic by remember(uiState.chatPic) {
        mutableStateOf(BitmapFactory.decodeFile(uiState.chatPic?.path))
    }

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        viewModel.startOrStopListening(false)
    }
    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.onPrimaryContainer)
        .fillMaxSize()
    ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BackButton (navController::navigateUp)
                Text(uiState.chat.title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    overflow = TextOverflow.Ellipsis)
                if (decodedPic != null) {
                    Image(bitmap = decodedPic!!.asImageBitmap(),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(chatPicSize)
                            .clip(CircleShape)
                            .clickable { navController.navigate("userProfile/${viewModel.otherUserId}") })
                } else {
                    Image(painter = painterResource(R.drawable.user),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(chatPicSize)
                            .clip(CircleShape)
                            .clickable { navController.navigate("userProfile/${viewModel.otherUserId}") })
                }
            }
            MessagesColumn(viewModel.userId, listState, uiState.messages, Modifier.weight(1f)) {chatId ->
                navController.navigate("chats/private/$chatId")
            }
            BottomRow(textFieldValue, onFieldValueChange = {textFieldValue = it},
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                viewModel.sendMessage(textFieldValue)
                textFieldValue = ""
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