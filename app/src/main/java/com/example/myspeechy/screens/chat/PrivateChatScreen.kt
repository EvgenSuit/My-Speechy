package com.example.myspeechy.screens.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myspeechy.R
import com.example.myspeechy.components.BackButton
import com.example.myspeechy.components.BottomRow
import com.example.myspeechy.components.MessagesColumn
import com.example.myspeechy.utils.chat.PrivateChatViewModel

@Composable
fun PrivateChatScreen(navController: NavHostController,
                      viewModel: PrivateChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    val chatPicSize = dimensionResource(R.dimen.chat_pic_size)
    val decodedPic = BitmapFactory.decodeFile(viewModel.chatPic.path)
    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BackButton (navController::navigateUp)
                Text(uiState.chat.title,
                    overflow = TextOverflow.Ellipsis)
                if (decodedPic != null) {
                    Image(bitmap = decodedPic.asImageBitmap(),
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
            }
            MessagesColumn(viewModel.userId, uiState.messages) {chatId ->
                navController.navigate("chats/private/$chatId")
            }
        }
        BottomRow(textFieldValue, onFieldValueChange = {textFieldValue = it},
                modifier = Modifier.align(Alignment.BottomCenter )) {
                viewModel.sendMessage(textFieldValue)
        }
    }
}