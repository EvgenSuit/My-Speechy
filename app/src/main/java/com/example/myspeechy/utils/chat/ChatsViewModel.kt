package com.example.myspeechy.utils.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.services.chat.ChatsServiceImpl
import com.example.myspeechy.services.chat.PrivateChatServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatsService: ChatsServiceImpl,
    private val privateChatServiceImpl: PrivateChatServiceImpl,
    @Named("ChatDataStore") private val chatDataStore: ChatDatastore,
    private val filesDirPath: String
): ViewModel() {
    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatsService.userId
    fun startOrStopListening(removeListeners: Boolean) {
        listOf("private", "public").forEach { type ->
            chatsService.checkIfHasChats(type) {hasChats ->
                if (!hasChats) {
                    _uiState.update { it.copy(chats = it.chats.filterValues { v -> v?.type != type }) }
                }
            }
        }
        listenForPublicChats(removeListeners)
        listenForPrivateChats(removeListeners)
    }
    private fun listenForPublicChats(remove: Boolean) {
        if (remove) {
            _uiState.value.chats.forEach {
                if (it.value!!.type == "public") {
                    listenForPublicChat(it.key!!, true)
                }
            }
        }
        chatsService.publicChatsStateListener(
            onAdded = {chatId ->
                listenForPublicChat(chatId, remove)
                viewModelScope.launch {
                    chatDataStore.addToChatList(userId)
                }
            },
            onChanged = {},
            onRemoved = {chatId ->
                viewModelScope.launch {
                    chatDataStore.removeFromChatList(userId)
                }
                listenForPublicChat(chatId, true)
                _uiState.update { it.copy(chats = it.chats.filterKeys { key -> key != null && key != chatId }) }
            },
            onCancelled = {},
            remove
        )
    }
    private fun listenForPublicChat(chatId: String, remove: Boolean) {
        chatsService.publicChatListener(chatId,
            {chat ->
                updateOrSortChats(chat.key!!, chat.getValue<Chat>(), true)
            },
            onCancelled = {},
            remove)
    }

    private fun listenForPrivateChats(remove: Boolean) {
        if (remove) {
            _uiState.value.chats.forEach {
                if (it.value!!.type == "private") {
                    listenForPrivateChat(it.key!!, true)
                }
            }
        }
        chatsService.privateChatsStateListener(
            onAdded = {chatId ->
                listenForPrivateChat(chatId, remove)
                val otherUserId = chatId.split("_").first { it != userId }
                listenForPrivateChatProfilePic(otherUserId, remove)
            },
            onChanged = {},
            onRemoved = {chatId ->
                listenForPrivateChat(chatId, true)
                val otherUserId = chatId.split("_").first { it != userId }
                listenForPrivateChatProfilePic(otherUserId, true)
            },
            onCancelled = {},
            remove
        )
    }
    private fun listenForPrivateChat(chatId: String, remove: Boolean) {
        chatsService.privateChatListener(chatId,
            {chat ->
                updateOrSortChats(chat.key!!, chat.getValue<Chat>(), true)
            },
            onCancelled = {},
            remove)
    }

    private fun listenForPrivateChatProfilePic(id: String, remove: Boolean) {
        val picDir = "${filesDirPath}/profilePics/${id}/"
        val picPath = "$picDir/lowQuality/$id.jpg"
        privateChatServiceImpl.chatProfilePictureListener(id, filesDirPath, {}, {
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
    fun leaveChat(chatId: String) {
        viewModelScope.launch {
            chatsService.leavePrivateChat(chatId)
        }
    }
    fun sortChatsOnStartup() {
        _uiState.value.chats.forEach {c ->
            updateOrSortChats(c.key!!, c.value, false)
        }
    }
    private fun updateOrSortChats(id: String, chat: Chat?, update: Boolean) {
        val newChatMap = if (update) _uiState.value.chats.toMutableMap().apply { this[id] = chat} else _uiState.value.chats
        _uiState.update { it.copy(chats = newChatMap
            .toSortedMap(compareByDescending { k -> newChatMap[k]?.timestamp }).filterValues { v -> v != null }) }
    }
    fun getChatPic(otherUserId: String): File = privateChatServiceImpl.getPic(filesDirPath, otherUserId)
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.formatStorageErrorMessage()) }
    }

    data class ChatsUiState(
        val searchedChat: Map<String, Chat> = mapOf(),
        val chats: Map<String?, Chat?> = mapOf(),
        val picPaths: Map<String, String> = mapOf(), //user id to pic path map
        val picsId: String = "",
        val storageErrorMessage: String = ""
    )
}