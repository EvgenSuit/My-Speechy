package com.example.myspeechy.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.Chat
import com.example.myspeechy.services.ChatsServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatsService: ChatsServiceImpl
): ViewModel() {
    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        chatsService.membershipListener({}) {membership ->
            if (membership.value != null) {
                (membership.value as Map<String, Map<String, String>>).keys.forEach { chatId ->
                    chatsService.chatsListener(chatId, {}) {chat ->
                    _uiState.update {
                        it.copy(chats = it.chats + mapOf(chatId to chat.getValue<Chat>()))
                    }
                }
                }
            }
        }
    }

    fun searchForChat(title: String) {
        chatsService.searchChatByTitle(title, {}) {chat ->
            if (chat.value != null) {
                val chatMap = chat.getValue<Map<String, Chat>>()
                val key = chatMap!!.keys.first()
                _uiState.update {
                    it.copy(searchedChat = mapOf(
                        key to chatMap[key]))
                }
            } else {
                _uiState.update {
                    ChatsUiState(chats = it.chats)
                }
            }
        }
    }

    data class ChatsUiState(val searchedChat: Map<String?, Chat?> = mapOf(),
        val chats: Map<String?, Chat?> = mapOf())
}