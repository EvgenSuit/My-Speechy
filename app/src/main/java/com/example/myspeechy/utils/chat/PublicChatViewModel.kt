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
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
open class PublicChatViewModel @Inject constructor(
    private val chatServiceImpl: PublicChatServiceImpl,
    private val filesDir: File,
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
                    listenForUsername(userId, remove)
                    listenForProfilePic(userId, remove)
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
                if (!_uiState.value.members.containsKey(userId)) _uiState.update { it.copy(joined = false)}
                m.keys.forEach {
                    listenForUsername(userId, true)
                    listenForProfilePic(userId, true)
                }
            },
            onCancelled = {},
            remove)
    }
    private fun listenForProfilePic(id: String, remove: Boolean) {
        if (id != userId) {
            val picDir = "${filesDir}/profilePics/${id}/"
            val picPath = "$picDir/lowQuality/$id.jpg"
            chatServiceImpl.usersProfilePicListener(id, filesDir.path, {}, {updateStorageErrorMessage(it)
                File(picPath).delete()
                File(picDir).deleteRecursively() }, {
                _uiState.update { it.copy(picPaths = it.picPaths.toMutableMap().apply { this[id] = picPath },
                    picsId = UUID.randomUUID().toString()) }
            }, remove)
        }
    }
    private fun listenForUsername(id: String, remove: Boolean) {
        chatServiceImpl.usernameListener(id, {}, {username ->
            val name = username.getValue<String>()
            if (name != null) {
                _uiState.update { it.copy(//members = it.members.mapValues { (k, v) -> if (k == id) name else ""},
                    messages = it.messages.mapValues { (_, v) ->
                        if (v.sender == id) v.copy(senderUsername = name) else v }) }
            }
        }, remove)
    }
        private fun listenForCurrentChat(remove: Boolean) {
            chatServiceImpl.chatListener(chatId, {}, { chat ->
                _uiState.update {
                    it.copy(chat = chat.getValue<Chat>() ?: Chat())
                }
            }, remove)
        }

        fun sendMessage(text: String, replyTo: String) {
            val chatTitle = _uiState.value.chat.title
            val timestamp = chatServiceImpl.sendMessage(chatId, _uiState.value.members.entries.first { it.key == userId }.value, text, replyTo)
            chatServiceImpl.updateLastMessage(chatId, Chat(chatTitle, text, timestamp))
        }
    fun editMessage(message: Map<String, Message>) {
        chatServiceImpl.editMessage(chatId, message)
    }
    fun deleteMessage(message: Map<String, Message>) {
        chatServiceImpl.deleteMessage(chatId, message)
    }

        fun joinChat() {
            chatServiceImpl.joinChat(chatId)
        }
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.split(" ").joinToString("_").uppercase(
            Locale.ROOT).dropLast(1)) }
    }


    data class PublicChatUiState(
        val messages: Map<String, Message> = mapOf(),
        val chat: Chat = Chat(),
        val members: Map<String, String> = mapOf(), //UserId to username map
        val errorCode: Int = 0,
        val joined: Boolean = false,
        val storageErrorMessage: String = "",
        val picPaths: Map<String, String> = mapOf(), //user id to pic path map
        val picsId: String = ""
    )
}