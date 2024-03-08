package com.example.myspeechy.utils.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.components.AlertDialogDataClass
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
        //listen for chats of which the current user is a member of
        listenForPublicChats(removeListeners)
        listenForPrivateChats(removeListeners)
        //listen for all public chats
        listenForAllPublicChats(removeListeners)
    }
    private fun listenForAllPublicChats(remove: Boolean) {
        chatsService.allPublicChatsListener(
            onAdded = {chat ->
                updateOrSortAllPublicChats(chat.keys.first(), chat.values.first())
            },
            onChanged = {chat ->
                updateOrSortAllPublicChats(chat.keys.first(), chat.values.first())
            },
            onRemoved = {chatId ->
                updateOrSortAllPublicChats(chatId)
            },
            onCancelled = {},
            remove
        )
    }
    private fun listenForPublicChats(remove: Boolean) {
        if (remove) {
            _uiState.value.chats.forEach {
                if (it.value != null && it.value!!.type == "public") {
                    listenForPublicChat(it.key, true)
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
                _uiState.update { it.copy(chats = it.chats.filterKeys { key -> key != chatId }) }
            },
            onCancelled = {},
            remove
        )
    }
    private fun listenForPublicChat(chatId: String, remove: Boolean) {
        chatsService.publicChatListener(chatId,
            {chat ->
                updateOrSortChats(chat.key!!, chat.getValue<Chat>())
            },
            onCancelled = {},
            remove)
    }

    private fun listenForPrivateChats(remove: Boolean) {
        if (remove) {
            _uiState.value.chats.forEach {
                if (it.value != null && it.value!!.type == "private") {
                    listenForPrivateChat(it.key, true)
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
                _uiState.update { it.copy(chats = it.chats.filterKeys { key -> key != chatId }) }
            },
            onCancelled = {},
            remove
        )
    }
    private fun listenForPrivateChat(chatId: String, remove: Boolean) {
        chatsService.privateChatListener(chatId,
            {chat ->
                updateOrSortChats(chat.key!!, chat.getValue<Chat>())
            },
            onCancelled = {},
            remove)
    }

    private fun listenForPrivateChatProfilePic(id: String, remove: Boolean) {
        val picDir = "${filesDirPath}/profilePics/${id}/"
        val picPath = "$picDir/lowQuality/$id.jpg"
        privateChatServiceImpl.chatProfilePictureListener(id, filesDirPath, {}, {
            updateStorageErrorMessage(it)
            File(picDir).deleteRecursively()
        }, {
            _uiState.update { it.copy(picPaths = it.picPaths.toMutableMap().apply { this[id] = picPath },
                picsId = UUID.randomUUID().toString()) }
        }, remove)
    }
    fun onNavigateToSearchedChat() {
        _uiState.update { it.copy(searchedChat = mapOf()) }
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
                    it.copy(searchedChat = mapOf())
                }
            }
        }
    }
    fun leaveChat(type: String, chatId: String) {
        viewModelScope.launch {
            if (type == "private") {
                chatsService.leavePrivateChat(chatId)
            }
            if (type == "public") {
                chatsService.checkIfIsAdmin(chatId) {isAdmin ->
                    if (isAdmin) {
                        _uiState.update { it.copy(alertDialogDataClass = AlertDialogDataClass(
                                title = "Are you sure?",
                                text = "If you leave the chat it will be deleted since you're an admin",
                                onConfirm = {chatsService.deletePublicChat(chatId) {
                                    viewModelScope.launch {
                                        chatsService.leavePublicChat(chatId)
                                        _uiState.update { it.copy(alertDialogDataClass = AlertDialogDataClass()) }
                                    }
                                }},
                                onDismiss = {_uiState.update { it.copy(alertDialogDataClass = AlertDialogDataClass()) }}
                            )) }
                    } else {
                        viewModelScope.launch {
                            chatsService.leavePublicChat(chatId)
                        }
                    }
                }
            }
        }
    }
    fun createPublicChat(title: String, description: String) {
        chatsService.createPublicChat(title, description)
    }
    fun sortAllPubicChatsOnStartup() {
        _uiState.value.allPublicChats.forEach {(id, chat) ->
            //updateOrSortAllPublicChats(id, chat)
        }
    }
    private fun updateOrSortChats(id: String, chat: Chat?) {
        val newChatMap = _uiState.value.chats.toMutableMap().apply { this[id] = chat}
        /* Sort by chat id (to account for a case where timestamps of multiple chats
         are identical) and timestamp */
        _uiState.update { it.copy(chats = newChatMap
            .toSortedMap(compareByDescending<String?> { k -> newChatMap[k]?.timestamp}.thenByDescending { k -> k })) }
    }
    private fun updateOrSortAllPublicChats(id: String, chat: Chat? = null) {
        val newChatMap = _uiState.value.allPublicChats.toMutableMap().apply { this[id] = chat}
        _uiState.update { it.copy(allPublicChats = newChatMap
            .toSortedMap(compareByDescending<String?> { k -> newChatMap[k]?.timestamp}.thenByDescending { k -> k })
            ) }
    }
    fun getChatPic(otherUserId: String): File = privateChatServiceImpl.getPic(filesDirPath, otherUserId)
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.formatStorageErrorMessage()) }
    }

    data class ChatsUiState(
        val searchedChat: Map<String, Chat> = mapOf(),
        val chats: Map<String, Chat?> = mapOf(),
        val allPublicChats: Map<String, Chat?> = mapOf(),
        val picPaths: Map<String, String> = mapOf(), //user id to pic path map
        val picsId: String = "",
        val alertDialogDataClass: AlertDialogDataClass = AlertDialogDataClass(),
        val storageErrorMessage: String = ""
    )
}