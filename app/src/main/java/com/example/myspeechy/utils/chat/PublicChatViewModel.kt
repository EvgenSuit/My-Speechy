package com.example.myspeechy.utils.chat

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
    init {
        listenForCurrentChat()
        listenForMessages()
        listenForChatMembers()
    }
    private fun listenForMessages() {
        chatServiceImpl.messagesListener(chatId, {errorCode ->
            _uiState.update {
                it.copy(errorCode = errorCode)
            } }) {messages ->
            for (snapshot in messages) {
                val messageId = snapshot.key!!
                val messageContent = snapshot.getValue<Message>() ?: Message()
                chatServiceImpl.usernameListener(messageContent.sender, {}) {senderUserName ->
                    val newMessages = _uiState.value.messages.toMutableMap()
                    newMessages[messageId] = messageContent
                        .copy(senderUsername = senderUserName.getValue<String>() ?: "")
                    _uiState.update {
                        it.copy(messages = newMessages, errorCode = 0)
                    }
                }
            }
        }
    }
    private fun listenForChatMembers() {
        chatServiceImpl.chatMembersListener(chatId, {}) {members ->
            _uiState.update {
                it.copy(members = members.map{ snapshot -> snapshot.key as String })
            }
            val joined = _uiState.value.members.contains(chatServiceImpl.userId)
            _uiState.update {
                it.copy(joined = joined, errorCode = if (joined) 0 else it.errorCode)
            }
        }
    }
    private fun listenForCurrentChat() {
        chatServiceImpl.chatListener(chatId, {}) {chat ->
            _uiState.update {
                it.copy(chat = chat.getValue<Chat>() ?: Chat())
            }
        }
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
        val members: List<String> = listOf(),
        val errorCode: Int = 0,
        val joined: Boolean = true,
        val isChatPublic: Boolean = false
    )
}