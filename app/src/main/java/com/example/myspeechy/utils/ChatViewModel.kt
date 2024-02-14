package com.example.myspeechy.utils

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.services.PrivateChatServiceImpl
import com.example.myspeechy.services.PublicChatServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    privateChatServiceImpl: PrivateChatServiceImpl,
    publicChatServiceImpl: PublicChatServiceImpl,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    private val chatServiceImpl = if (checkNotNull(savedStateHandle["type"]) == "public") publicChatServiceImpl else privateChatServiceImpl

    private fun listenForMessages() {
        chatServiceImpl.messagesListener(chatId, {errorCode ->
            _uiState.update {
                it.copy(errorCode = errorCode)
            }
        }) {messages ->
            val newMessages = buildMap(messages.size) {
                for (snapshot in messages) {
                    this[snapshot.key!!] = snapshot.getValue<Message>() ?: Message()
                }
            }
            _uiState.update {
                it.copy(messages = newMessages, errorCode = 0)
            }
        }
    }
    private fun listenForChatMembers() {
        chatServiceImpl.chatMembersListener(chatId, {}) {members ->
            _uiState.update {
                it.copy(members = members.map{ snapshot -> snapshot.key as String })
            }
            _uiState.update {
                it.copy(joined = _uiState.value.members.contains(chatServiceImpl.userId))
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
    fun joinChat() {
        chatServiceImpl.joinChat(chatId)
    }

    init {
        _uiState.update { it.copy(isChatPublic = chatServiceImpl is PublicChatServiceImpl) }
        listenForCurrentChat()
        listenForMessages()
        listenForChatMembers()
    }

    fun sendMessage(text: String) {
        //If error code is -3, the user has not jet joined a chat
        if (_uiState.value.errorCode == -3) {
            joinChat()
            /*Call listener again because if there was an error,
            the previous one was cancelled*/
            listenForMessages()
        }
        val chatTitle = _uiState.value.chat.title
        val timestamp = chatServiceImpl.sendMessage(chatId, chatTitle, text)
        chatServiceImpl.updateLastMessage(chatId, Chat(chatTitle, text, timestamp))
    }

    fun getUserId(): String = chatServiceImpl.userId

    data class ChatUiState(
        val messages: Map<String, Message>? = mapOf(),
        val chat: Chat = Chat(),
        val members: List<String> = listOf(),
        val errorCode: Int = 0,
        val joined: Boolean = true,
        val isChatPublic: Boolean = false
    )
}