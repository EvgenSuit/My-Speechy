package com.example.myspeechy.utils.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.services.chat.PictureStorageError
import com.example.myspeechy.services.chat.PrivateChatServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PrivateChatViewModel @Inject constructor(
    private val chatServiceImpl: PrivateChatServiceImpl,
    private val filesDirPath: String,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val _uiState = MutableStateFlow(PrivateChatUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatServiceImpl.userId
    val otherUserId = chatId.split("_").first { it != userId }
    val picRef = File("$filesDirPath/profilePics/$otherUserId/lowQuality/$otherUserId.jpg")

    fun startOrStopListening(removeListeners: Boolean) {
        listenForCurrentChat(removeListeners)
        listenForMessages(removeListeners)
        listenForProfilePic(removeListeners)
        listenIfIsMemberOfChat(removeListeners)
        listOf(userId, otherUserId).forEach {
            listenForUsername(it, removeListeners)
        }
    }
    private fun listenIfIsMemberOfChat(remove: Boolean) {
        chatServiceImpl.listenIfIsMemberOfChat(chatId, {isMemberOfChat ->
            _uiState.update { it.copy(isMemberOfChat = isMemberOfChat) }
        }, remove)
    }

    private fun listenForUsername(id: String, remove: Boolean) {
        chatServiceImpl.usernameListener(id, {}, {username ->
            val name = username.getValue<String>() ?: ""
            val messages = _uiState.value.messages.toMutableMap()
            messages.forEach {
                if (it.value.sender == id) {
                    it.value.senderUsername = name
                }
            }
            _uiState.update {
                it.copy(messages = messages.toMap())
            }
            _uiState.update {
                if (id == userId) it.copy(currUsername = name) else it.copy(otherUsername = name)
            }
        }, remove)
    }

    private fun listenForProfilePic(remove: Boolean) {
        chatServiceImpl.chatProfilePictureListener(otherUserId, filesDirPath, {}, { m ->
            updateStorageErrorMessage(m)
            val errorMessage = _uiState.value.storageErrorMessage
            if (PictureStorageError.OBJECT_DOES_NOT_EXIST_AT_LOCATION.name.contains(errorMessage) ||
                PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name.contains(errorMessage)) {
                _uiState.update { it.copy(picId = UUID.randomUUID().toString()) }
                picRef.delete()
            }
            _uiState.update { it.copy(storageErrorMessage = m, picId = UUID.randomUUID().toString()) }
        }, {
            _uiState.update { it.copy(storageErrorMessage = "", picId = UUID.randomUUID().toString()) }
        }, remove)
    }
    private fun listenForMessages(remove: Boolean) {
        chatServiceImpl.messagesListener(chatId,
            onAdded = {m ->
                val id = m.keys.first()
                val savedMessage = _uiState.value.messages[id]
                if (savedMessage != null) {
                    _uiState.update { it.copy(it.messages + mapOf(id to m.values.first().copy(senderUsername = savedMessage.senderUsername))) }
                } else {
                    _uiState.update { it.copy(it.messages + m)}
                }
            },
            onChanged = {m ->
                _uiState.update { it.copy(messages = it.messages.toMutableMap().apply { this[m.keys.first()] = m.values.first()})}
            },
            onRemoved = {m ->
                        _uiState.update { it.copy(messages = it.messages.filterKeys { key -> key != m.keys.first() }) }
            },
            onCancelled = {updateErrorCode(it)
                          if (it == -3) {
                              _uiState.update { it.copy(messages = mapOf()) }
                          } },
            remove)
    }
    private fun listenForCurrentChat(remove: Boolean) {
        chatServiceImpl.chatListener(chatId, {updateErrorCode(it)}, {chat ->
            val value = chat.getValue<Chat>()
            if (value != null) {
                _uiState.update {
                    it.copy(chat = value)
                }
            }
        }, remove)
    }
    fun sendMessage(text: String, replyTo: String) {
        //If messages are empty, the user has not jet joined the chat
        if (!_uiState.value.isMemberOfChat) {
            chatServiceImpl.joinChat(chatId)
            /*Call listeners again because if there was an error,
            the previous ones were cancelled*/
            listenForMessages(true)
            listenForMessages(false)
        }
        val timestamp = chatServiceImpl.sendMessage(chatId, _uiState.value.currUsername, text, replyTo)
        chatServiceImpl.updateLastMessage(chatId, _uiState.value.currUsername,
            _uiState.value.otherUsername,
            Chat(lastMessage = text, timestamp =  timestamp))
    }
    fun editMessage(message: Map<String, Message>) {
        chatServiceImpl.editMessage(chatId, message)
        chatServiceImpl.updateLastMessage(chatId, _uiState.value.currUsername,
            _uiState.value.otherUsername,
            _uiState.value.chat.copy(lastMessage = message.values.first().text))
    }
    fun deleteMessage(message: Map<String, Message>) {
        val messages = _uiState.value.messages
        val entries = messages.entries
        chatServiceImpl.deleteMessage(chatId, message)
        if (entries.last().value == message.values.first() && entries.size > 1) {
            val prevMessage = messages.values.toList()[messages.values.toList().indexOf(message.values.first())-1]
            chatServiceImpl.updateLastMessage(chatId, _uiState.value.currUsername,
                _uiState.value.otherUsername,
                _uiState.value.chat.copy(prevMessage.sender, prevMessage.text,
                prevMessage.timestamp))
        } else if (entries.size <= 1) {
            viewModelScope.launch {
                chatServiceImpl.leaveChat(chatId)
            }
        }
    }
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.formatStorageErrorMessage()) }
    }
    private fun updateErrorCode(code: Int) {
        _uiState.update { it.copy(errorCode = code) }
    }

    data class PrivateChatUiState(
        val messages: Map<String, Message> = mapOf(),
        val chat: Chat = Chat(),
        val isMemberOfChat: Boolean = false,
        val picId: String = "",
        val currUsername: String = "",
        val otherUsername: String = "",
        val storageErrorMessage: String = "",
        val errorCode: Int = 0,
        val removeListeners: Boolean = false
    )
}