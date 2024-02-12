package com.example.myspeechy.utils

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.Chat
import com.example.myspeechy.data.Message
import com.example.myspeechy.services.ChatServiceImpl
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.getValue
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatServiceImpl: ChatServiceImpl,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()


    fun listen() {
        chatServiceImpl.chatListener(chatId, {}) {chat ->
            Log.d("CHAT", chat.toString())
            _uiState.update {
                it.copy(chat = chat.getValue<Chat>() ?: Chat())
            }
        }
        chatServiceImpl.messagesListener(chatId, {}) {messages ->
            val newMessages = buildMap(messages.size) {
                for (snapshot in messages) {
                    this[snapshot.key!!] = snapshot.getValue<Message>() ?: Message()
                }
            }
            _uiState.update {
                it.copy(messages = newMessages)
            }
            /*Log.d("MESSAGES",
                _uiState.value.messages?.toSortedMap(Comparator.reverseOrder()).toString())*/
        }
    }

    init {
        listen()
    }

    fun sendMessage(text: String) {
        chatServiceImpl.sendMessage(chatId, _uiState.value.chat.title, text)
    }

    data class ChatUiState(val messages: Map<String, Message>? = mapOf(),
        val chat: Chat = Chat())
}