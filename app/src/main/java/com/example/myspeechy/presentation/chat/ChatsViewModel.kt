package com.example.myspeechy.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.components.AlertDialogDataClass
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.domain.chat.ChatsServiceImpl
import com.example.myspeechy.domain.chat.PrivateChatServiceImpl
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
class ChatsViewModel @Inject constructor(
    private val chatsService: ChatsServiceImpl,
    private val privateChatServiceImpl: PrivateChatServiceImpl,
    private val filesDirPath: String
): ViewModel() {
    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatsService.userId

    init {
        Log.d("CHATS VIEW MODEL", "HERE")
    }
    fun startOrStopListening(removeListeners: Boolean) {
        viewModelScope.launch {
            try {
                listOf("private", "public").forEach { type ->
                    val hasChats = chatsService.checkIfHasChats(type)
                    if (!hasChats) {
                        _uiState.update { it.copy(chats = it.chats.filterValues { v -> v?.type != type }) }
                    }
                }
            } catch (e: Exception) {
                updateChatsErrorMessage(e.message!!)
            }
        }
        //listen for chats of which the current user is a member of
        listenForPublicChats(removeListeners)
        listenForPrivateChats(removeListeners)
        //listen for all public chats
        if (removeListeners) {
            listenForAllPublicChats(remove = true)
            updateChatsErrorMessage("")
        }
    }
    fun handleDynamicAllChatsLoading(loadOnResume: Boolean = false,
                                      firstVisibleChatIndex: Int?) {
            chatsService.handleDynamicAllChatsLoading(loadOnResume,
                _uiState.value.maxChatBatchIndex,
                firstVisibleChatIndex,
                onRemove = {listenForAllPublicChats(remove = true)},
                onLoad = {listenForAllPublicChats(it, false)})
    }
    private fun listenForAllPublicChats(firstIndex: Int = 0, remove: Boolean) {
        if (!remove) {
            _uiState.update { it.copy(maxChatBatchIndex = it.maxChatBatchIndex + firstIndex) }
        }
            chatsService.allPublicChatsListener(
                _uiState.value.maxChatBatchIndex,
                onAdded = {chat ->
                    updateOrSortAllPublicChats(chat.keys.first(), chat.values.first())
                },
                onChanged = {chat ->
                    updateOrSortAllPublicChats(chat.keys.first(), chat.values.first())
                },
                onRemoved = {chatId ->
                    updateOrSortAllPublicChats(chatId)
                },
                onCancelled = {updateChatsErrorMessage(it)},
                remove
            )
    }
    private fun listenForPublicChats(remove: Boolean) {
        try {
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
                },
                onChanged = {},
                onRemoved = {chatId ->
                    listenForPublicChat(chatId, true)
                    _uiState.update { it.copy(chats = it.chats.filterKeys { key -> key != chatId }) }
                },
                onCancelled = { updateChatsErrorMessage(it) },
                remove
            )
        } catch (e: Exception) {
            updateChatsErrorMessage(e.message!!)
        }
    }
    private fun listenForPublicChat(chatId: String, remove: Boolean) {
        try {
            chatsService.publicChatListener(chatId,
                {chat ->
                    updateOrSortChats(chat.key!!, chat.getValue(Chat::class.java))
                },
                onCancelled = {updateChatsErrorMessage(it)},
                remove)
        } catch (e: Exception) {
            updateChatsErrorMessage(e.message!!)
        }
    }

    private fun listenForPrivateChats(remove: Boolean) {
        try {
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
                onCancelled = {updateChatsErrorMessage(it)},
                remove
            )
        } catch (e: Exception) {
            updateChatsErrorMessage(e.message!!)
        }
    }
    private fun listenForPrivateChat(chatId: String, remove: Boolean) {
        try {
            chatsService.privateChatListener(chatId,
                {chat ->
                    updateOrSortChats(chat.key!!, chat.getValue(Chat::class.java))
                },
                onCancelled = {updateChatsErrorMessage(it)},
                remove)
        } catch (e: Exception) {
            updateChatsErrorMessage(e.message!!)
        }
    }

    private fun listenForPrivateChatProfilePic(id: String, remove: Boolean) {
        val picDir = "${filesDirPath}/profilePics/${id}/"
        val picPath = "$picDir/lowQuality/$id.jpg"
        try {
            privateChatServiceImpl.chatProfilePictureListener(id, filesDirPath, { updateChatsErrorMessage(it.message) }, {
                updateStorageErrorMessage(it)
                File(picDir).deleteRecursively()
            }, {
                _uiState.update { it.copy(picPaths = it.picPaths.toMutableMap().apply { this[id] = picPath },
                    picsId = UUID.randomUUID().toString()) }
            }, remove)
        } catch (e: Exception) {
            updateChatsErrorMessage(e.message!!)
        }
    }
    fun clearSearchedChats() {
        _uiState.update { it.copy(searchedChats = mapOf()) }
    }
    fun searchForChat(title: String) {
        viewModelScope.launch {
            try {
                val chat = chatsService.searchChatByTitle(title)
                if (chat.isNotEmpty()) {
                    chat.forEach { (k, v) ->
                        if (v != null) {
                            _uiState.update {
                                it.copy(searchedChats = it.searchedChats + mapOf(k to v))
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(searchedChats = mapOf())
                    }
                }
            } catch (e: Exception) {
                updateChatsErrorMessage(e.message!!)
            }
        }
    }
    fun leaveChat(type: String, chatId: String) {
        viewModelScope.launch {
            try {
                if (type == "private") {
                    chatsService.leavePrivateChat(chatId)
                }
                if (type == "public") {
                    val isAdmin = chatsService.checkIfIsAdmin(chatId)
                    if (isAdmin) {
                        _uiState.update { it.copy(
                            chatsError = "",
                            alertDialogDataClass = AlertDialogDataClass(
                            title = "Are you sure?",
                            text = "If you leave the chat it will be deleted since you're its admin",
                            onConfirm = {viewModelScope.launch {
                                try {
                                    _uiState.update { it.copy(alertDialogDataClass = AlertDialogDataClass()) }
                                    chatsService.deletePublicChat(chatId)
                                } catch (e: Exception) {
                                    updateChatsErrorMessage(e.message!!)
                                }
                            }
                            },
                            onDismiss = {_uiState.update { it.copy(alertDialogDataClass = AlertDialogDataClass()) }}
                        )) }
                    } else {
                        chatsService.leavePublicChat(chatId, true)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(alertDialogDataClass = AlertDialogDataClass()) }
                updateChatsErrorMessage(e.message!!)
            }
        }
    }
    fun formatDate(timestamp: Long): String = chatsService.formatDate(timestamp)
    fun createPublicChat(title: String, description: String) {
        viewModelScope.launch {
            try {
                chatsService.createPublicChat(title, description)
            } catch (e: Exception) {
                updateChatsErrorMessage(e.message!!)
            }
        }
    }
    private fun updateOrSortChats(id: String, chat: Chat?) {
        val newChatMap = _uiState.value.chats.toMutableMap().apply { this[id] = chat}
        /* Sort by chat id (to account for a case where timestamps of multiple chats
         are identical) and timestamp */
        _uiState.update { it.copy(chats = newChatMap
            .toSortedMap(compareByDescending<String?> { k -> newChatMap[k]?.timestamp}.thenByDescending { k -> k })
            .filterValues { v -> v != null && v.title.isNotEmpty() }) }

    }
    private fun updateOrSortAllPublicChats(id: String, chat: Chat? = null) {
        val newChatMap = _uiState.value.allPublicChats.toMutableMap().apply { this[id] = chat}
        _uiState.update { it.copy(allPublicChats = newChatMap
            .toSortedMap(compareByDescending<String?> { k -> newChatMap[k]?.timestamp}.thenByDescending { k -> k })
            .filterValues { v -> v != null && v.title.isNotEmpty() }) }
    }
    fun getChatPic(otherUserId: String): File = privateChatServiceImpl.getPic(filesDirPath, otherUserId)
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.formatStorageErrorMessage()) }
    }
    private fun updateChatsErrorMessage(m: String) {
        _uiState.update { it.copy(chatsError = m) }
    }

    data class ChatsUiState(
        val searchedChats: Map<String, Chat> = mapOf(),
        val chats: Map<String, Chat?> = mapOf(),
        val allPublicChats: Map<String, Chat?> = mapOf(),
        val picPaths: Map<String, String> = mapOf(), //user id to pic path map
        val picsId: String = "",
        val maxChatBatchIndex: Int = 0,
        val alertDialogDataClass: AlertDialogDataClass = AlertDialogDataClass(),
        val storageErrorMessage: String = "",
        val chatsError: String = ""
    )
}