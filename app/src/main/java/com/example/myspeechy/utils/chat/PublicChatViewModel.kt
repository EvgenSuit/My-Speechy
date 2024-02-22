package com.example.myspeechy.utils.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.services.chat.PublicChatServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
open class PublicChatViewModel @Inject constructor(
    private val chatServiceImpl: PublicChatServiceImpl,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val _uiState = MutableStateFlow(PublicChatUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatServiceImpl.userId
    fun startOrStopListening(removeListeners: Boolean) {
        listenForCurrentChat(removeListeners)
        listenForMessages(removeListeners)
        listenForChatMembers(removeListeners)
    }
    private fun listenForMessages(remove: Boolean) {
        chatServiceImpl.messagesListener(chatId,
            onAdded = {m ->
                _uiState.update { it.copy(it.messages + m) }
            },
            onChanged = {m ->
                _uiState.update { it.copy(messages = it.messages.toMutableMap().apply { this[m.keys.first()] = m.values.first()})}
            },
            onRemoved = {m ->
                _uiState.update { it.copy(messages = it.messages.filterKeys { key -> key != m.keys.first() }) }
            },
            onCancelled = {},
            remove)
    }
    private fun listenForChatMembers(remove: Boolean) {
        if (remove) {
            _uiState.value.members.forEach {
                chatServiceImpl.usernameListener(it.key, {}, {}, true ) }
        }
        chatServiceImpl.chatMembersListener(chatId,
            onAdded = {m ->
                m.keys.forEach { userId ->
                    chatServiceImpl.usernameListener(userId, {}, {username ->
                        _uiState.update { it.copy(messages = it.messages.mapValues { (_, v) ->
                            if (v.sender == userId) v.copy(senderUsername = username.getValue<String>()) else v }) }
                    }, remove)
                }
                _uiState.update { it.copy(members = it.members + m, joined = (it.members + m).containsKey(userId)) }
            },
            onChanged = {m ->
                val id = m.keys.first()
                val value = m.values.first()
                _uiState.update { it.copy(members = it.members.toMutableMap().apply { this[id] = value },
                    messages = it.messages.mapValues { (key, v) ->
                        if (key == id) v.copy(senderUsername = value) else v }) }
            },
            onRemoved = {m ->
                _uiState.update { it.copy(members = it.members.filterKeys { key -> key != m.keys.first() }) }
            },
            onCancelled = {},
            remove)
    }
        private fun listenForCurrentChat(remove: Boolean) {
            chatServiceImpl.chatListener(chatId, {}, { chat ->
                _uiState.update {
                    it.copy(chat = chat.getValue<Chat>() ?: Chat())
                }
            }, remove)
        }

        fun sendMessage(text: String) {
            val chatTitle = _uiState.value.chat.title
            val timestamp = chatServiceImpl.sendMessage(chatId, chatTitle, text)
            chatServiceImpl.updateLastMessage(chatId, Chat(chatTitle, text, timestamp))
        }

        fun joinChat() {
            chatServiceImpl.joinChat(chatId)
        }


    data class PublicChatUiState(
        val messages: Map<String, Message> = mapOf(),
        val chat: Chat = Chat(),
        //UserId to username map
        val members: Map<String, String> = mapOf(),
        val errorCode: Int = 0,
        val joined: Boolean = true,
        val isChatPublic: Boolean = false,
        val removeListeners: Boolean = false
    )
}