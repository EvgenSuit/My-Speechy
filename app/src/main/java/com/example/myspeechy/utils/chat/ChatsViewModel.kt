package com.example.myspeechy.utils.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.services.chat.ChatsServiceImpl
import com.example.myspeechy.services.chat.PrivateChatServiceImpl
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
class ChatsViewModel @Inject constructor(
    private val chatsService: ChatsServiceImpl,
    private val privateChatServiceImpl: PrivateChatServiceImpl,
    private val filesDir: File
): ViewModel() {
    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatsService.userId
    fun startOrStopListening(removeListeners: Boolean) {
        listenForPublicChats(removeListeners)
        listenForPrivateChats(removeListeners)
    }
    private fun listenForPublicChats(remove: Boolean) {
        if (remove) {
            _uiState.value.chats.forEach {
                if (it.value!!.type == "public") {
                    publicChatListener(it.key!!, true)
                }
            }
        }
        chatsService.publicChatsStateListener(
            onAdded = {chatId ->
                publicChatListener(chatId, remove)
            },
            onChanged = {},
            onRemoved = {chatId ->
                publicChatListener(chatId, true)
                _uiState.value.chats.forEach { publicChatListener(it.key!!, true) }
                _uiState.update { it.copy(chats = it.chats.filterKeys { key -> key != null && key != chatId }) }
            },
            onCancelled = {},
            remove
        )
    }
    private fun publicChatListener(chatId: String, remove: Boolean) {
        chatsService.publicChatListener(chatId,
            {chat ->
                _uiState.update { it.copy(chats = it.chats.toMutableMap().apply { this[chat.key] = chat.getValue<Chat>()}) }
            },
            onCancelled = {},
            remove)
    }

    private fun listenForPrivateChats(remove: Boolean) {
        if (remove) {
            _uiState.value.chats.forEach {
                if (it.value!!.type == "private") {
                    privateChatListener(it.key!!, true)
                }
            }
        }
        chatsService.privateChatsStateListener(
            onAdded = {chatId ->
                privateChatListener(chatId, remove)
                val otherUserId = chatId.split("_").first { it != userId }
                listenForPrivateChatProfilePic(otherUserId, remove)
            },
            onChanged = {},
            onRemoved = {chatId ->
                privateChatListener(chatId, true)
                val otherUserId = chatId.split("_").first { it != userId }
                listenForPrivateChatProfilePic(otherUserId, true)
                _uiState.value.chats.forEach { privateChatListener(it.key!!, true) }
            },
            onCancelled = {},
            remove
        )
    }
    private fun privateChatListener(chatId: String, remove: Boolean) {
        chatsService.privateChatListener(chatId,
            {chat ->
                _uiState.update { it.copy(chats = it.chats.toMutableMap().apply { this[chat.key] = chat.getValue<Chat>()}) }
            },
            onCancelled = {},
            remove)
    }
    private fun listenForPrivateChatProfilePic(id: String, remove: Boolean) {
        val picDir = "${filesDir}/profilePics/${id}/"
        val picPath = "$picDir/lowQuality/$id.jpg"
        privateChatServiceImpl.chatProfilePictureListener(id, filesDir.path, {}, {
            updateStorageErrorMessage(it)
            File(picPath).delete()
            File(picDir).deleteRecursively()
        }, {
            _uiState.update { it.copy(picsId = UUID.randomUUID().toString()) }
        }, remove)
    }
    fun searchForChat(title: String) {
        chatsService.searchChatByTitle(title, {}) {chat ->
            if (chat.value != null) {
                val chatMap = chat.getValue<Map<String, Chat>>()
                val key = chatMap!!.keys.first()
                _uiState.update {
                    it.copy(searchedChat = mapOf(
                        key to (chatMap[key] ?: Chat())))
                }
            } else {
                _uiState.update {
                    ChatsUiState(chats = it.chats)
                }
            }
        }
    }
    fun getChatPic(otherUserId: String): File = privateChatServiceImpl.getPic(filesDir.path, otherUserId)
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.split(" ").joinToString("_").uppercase(
            Locale.ROOT).dropLast(1)) }
    }

    data class ChatsUiState(
        val searchedChat: Map<String, Chat> = mapOf(),
        val chats: Map<String?, Chat?> = mapOf(),
        val picPaths: Map<String, String> = mapOf(), //user id to pic path map
        val picsId: String = "",
        val storageErrorMessage: String = ""
    )
}