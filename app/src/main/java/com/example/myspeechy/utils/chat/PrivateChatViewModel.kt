package com.example.myspeechy.utils.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.services.chat.PictureStorageError
import com.example.myspeechy.services.chat.PrivateChatServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Locale
import java.util.UUID
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
    val otherUserId = chatId.split("_").filter { id -> id != userId }[0]
    val picRef = File("$filesDir/profilePics/$otherUserId/lowQuality/$otherUserId.jpg")

    fun startOrStopListening(removeListeners: Boolean) {
        if (!removeListeners) {
            _uiState.update { it.copy(picPath = picRef.path) }
        }
        listenForCurrentChat(removeListeners)
        listenForMessages(removeListeners)
        listenForProfilePic(removeListeners)
        listenForUsername(removeListeners)
    }

    private fun listenForUsername(remove: Boolean) {
        chatServiceImpl.usernameListener(userId, {}, {username ->
            val name = username.getValue<String>() ?: ""
            val messages = _uiState.value.messages.toMutableMap()
            messages.forEach { if (it.value.sender == userId) it.value.senderUsername = name}
            _uiState.update {
                it.copy(messages = messages.toMap(), currUsername = name)
            }
        }, remove)
    }

    private fun listenForProfilePic(remove: Boolean) {
        chatServiceImpl.chatProfilePictureListener(otherUserId, filesDir.path, {}, {m ->
            updateStorageErrorMessage(m)
            val errorMessage = _uiState.value.storageErrorMessage
            Log.d("ERROR", errorMessage)
            if (PictureStorageError.OBJECT_DOES_NOT_EXIST_AT_LOCATION.name.contains(errorMessage) ||
                PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name.contains(errorMessage)) {
                _uiState.update { it.copy(picPath = null, picId = UUID.randomUUID().toString()) }
            }
            picRef.delete()
            _uiState.update { it.copy(storageErrorMessage = m, picPath = null, picId = UUID.randomUUID().toString()) }
        }, {
            _uiState.update { it.copy(storageErrorMessage = "", picPath = picRef.path, picId = UUID.randomUUID().toString()) }
        }, remove)
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
            onCancelled = {updateErrorCode(it)
                          if (it == -3) {
                              _uiState.update { it.copy(messages = mapOf()) }
                          }
                          },
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
    fun sendMessage(text: String) {
        //If error code is -3, the user has not jet joined a chat
        if (_uiState.value.errorCode == -3) {

            chatServiceImpl.joinChat(chatId)
            /*Call listener again because if there was an error,
            the previous one was cancelled*/
            listenForMessages(true)
            listenForMessages(false)
        }
        val chatTitle = _uiState.value.chat.title
        val timestamp = chatServiceImpl.sendMessage(chatId, _uiState.value.currUsername, text)
        chatServiceImpl.updateLastMessage(chatId, Chat(chatTitle, text, timestamp))
    }
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.split(" ").joinToString("_").uppercase(
            Locale.ROOT).dropLast(1)) }
    }
    private fun updateErrorCode(code: Int) {
        _uiState.update { it.copy(errorCode = code) }
    }

    data class PrivateChatUiState(
        val messages: Map<String, Message> = mapOf(),
        val chat: Chat = Chat(),
        val picPath: String? = null,
        val picId: String = "",
        val currUsername: String = "",
        val storageErrorMessage: String = "",
        val errorCode: Int = 0,
        val removeListeners: Boolean = false
    )
}