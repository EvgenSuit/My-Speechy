package com.example.myspeechy.utils.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.services.chat.PrivateChatServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@HiltViewModel
open class PrivateChatViewModel @Inject constructor(
    private val chatServiceImpl: PrivateChatServiceImpl,
    private val filesDir: File,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val _uiState = MutableStateFlow(PrivateChatUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatServiceImpl.userId
    private val otherUserId = chatId.split("_").filter { id -> id != userId }[0]
    val chatPic = chatServiceImpl.getChatPic(filesDir.path, otherUserId)

    init {
        chatServiceImpl.usernameListener(otherUserId, {}) {otherUsername ->
            _uiState.update {
                it.copy(chat = it.chat.copy(title = otherUsername.getValue<String>() ?: ""))
            }
        }
        listenForCurrentChat()
        listenForMessages()
        listenForProfilePic()
    }
    private fun listenForProfilePic() {
        chatServiceImpl.chatProfilePictureListener(otherUserId, filesDir.path, {}, {m ->
            _uiState.update { it.copy(storageErrorMessage = m) }
        }) {
            _uiState.update { it.copy(storageErrorMessage = "") }
        }
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
                        //Update only those messages that's content has changed or is empty
                        if (newMessages[messageId]?.text != messageContent.text ||
                            newMessages[messageId]?.text.isNullOrEmpty()) {
                            newMessages[messageId] = messageContent
                                .copy(senderUsername = senderUserName.getValue<String>() ?: "")
                            _uiState.update {
                                it.copy(messages = newMessages, errorCode = 0)
                            }
                        }
                    }
            }
        }
    }
    private fun listenForCurrentChat() {
        chatServiceImpl.chatListener(chatId, {}) {chat ->
            val value = chat.getValue<Chat>()
            if (value != null) {
                _uiState.update {
                    it.copy(chat = value)
                }
            }
        }
    }
    fun sendMessage(text: String) {
        //If error code is -3, the user has not jet joined a chat
        if (_uiState.value.errorCode == -3) {
            chatServiceImpl.joinChat(chatId)
            /*Call listener again because if there was an error,
            the previous one was cancelled*/
            listenForMessages()
        }
        val chatTitle = _uiState.value.chat.title
        val timestamp = chatServiceImpl.sendMessage(chatId, chatTitle, text)
        chatServiceImpl.updateLastMessage(chatId, Chat(chatTitle, text, timestamp))
    }

    data class PrivateChatUiState(
        val messages: Map<String, Message> = mapOf(),
        val chat: Chat = Chat(),
        val storageErrorMessage: String = "",
        val errorCode: Int = 0
    )
}