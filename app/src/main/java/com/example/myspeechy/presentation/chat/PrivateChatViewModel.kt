package com.example.myspeechy.presentation.chat

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.data.chat.MessagesState
import com.example.myspeechy.domain.chat.PrivateChatServiceImpl
import com.example.myspeechy.domain.error.PictureStorageError
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        listenForProfilePic(removeListeners)
        checkIfChatIsEmpty(removeListeners)
        listenIfIsMemberOfChat(removeListeners)
        listOf(userId, otherUserId).forEach {
            listenForUsername(it, removeListeners)
        }
        if (removeListeners) {
            listenForMessages(remove = true)
            updateErrorMessage("")
        }
    }
    private fun checkIfChatIsEmpty(remove: Boolean) {
        chatServiceImpl.checkIfChatIsEmpty(chatId, remove, {updateErrorMessage(it.message)}) {isEmpty ->
            _uiState.update { it.copy(messagesState = if (isEmpty) MessagesState.EMPTY else MessagesState.IDLE) }
        }
    }
    fun handleDynamicMessageLoading(loadOnResume: Boolean = false, lastVisibleItemIndex: Int?) {
        chatServiceImpl.handleDynamicMessageLoading(
            loadOnActivityResume = loadOnResume,
            topMessageIndex = _uiState.value.topMessageBatchIndex,
            lastVisibleItemIndex = lastVisibleItemIndex,
            onRemove = {listenForMessages(remove = true)},
            onLoad = {topIndex ->
                listenForMessages(topIndex, false)})
    }
    private fun listenIfIsMemberOfChat(remove: Boolean) {
        if (userId == null) return
        chatServiceImpl.listenIfIsMemberOfChat(userId, chatId,
            {updateErrorMessage(it)},
            {isMemberOfChat ->
            _uiState.update { it.copy(isMemberOfChat = isMemberOfChat) }
        }, remove)
    }

    private fun listenForUsername(id: String?, remove: Boolean) {
        chatServiceImpl.usernameListener(id, {
            updateErrorMessage(it.message)
        }, {username ->
            val name = username.getValue(String::class.java)
            _uiState.update {
                it.copy(messages = it.messages.mapValues { (_, v) -> if (v.sender == id) v.copy(senderUsername = name) else v })
            }
            _uiState.update {
                if (id == userId) it.copy(currUsername = name) else it.copy(otherUsername = name)
            }
        }, remove)
    }

    private fun listenForProfilePic(remove: Boolean) {
        chatServiceImpl.chatProfilePictureListener(otherUserId, filesDirPath, {
            updateErrorMessage(it.message)
        }, { m ->
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
    private fun listenForMessages(topIndex: Int = 0, remove: Boolean) {
        if (!remove) {
            _uiState.update { it.copy(topMessageBatchIndex = it.topMessageBatchIndex + topIndex) }
        }
        if (_uiState.value.messagesState != MessagesState.EMPTY) {
            _uiState.update { it.copy(messagesState = MessagesState.LOADING) }
            chatServiceImpl.messagesListener(
                chatId, topIndex,
                onAdded = { m ->
                    val id = m.keys.first()
                    val savedMessage = _uiState.value.messages[id]
                    if (savedMessage != null) {
                        _uiState.update {
                            it.copy(
                                it.messages + mapOf(
                                    id to m.values.first()
                                        .copy(senderUsername = savedMessage.senderUsername)
                                )
                            )
                        }
                    } else {
                        _uiState.update { it.copy(it.messages + m) }
                    }
                    _uiState.update { it.copy(messagesState = MessagesState.IDLE) }
                },
                onChanged = { m ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages.toMutableMap()
                                .apply { this[m.keys.first()] = m.values.first() })
                    }
                },
                onRemoved = { m ->
                    _uiState.update { it.copy(messages = it.messages.filterKeys { key -> key != m.keys.first() }) }
                },
                onCancelled = {
                    updateErrorMessage(it)
                },
                remove
            )
        }
    }
    private fun listenForCurrentChat(remove: Boolean) {
        chatServiceImpl.chatListener(chatId, {updateErrorMessage(it.message)}, {chat ->
            val value = chat.getValue(Chat::class.java)
            if (value != null) {
                _uiState.update {
                    it.copy(chat = value)
                }
            }
        }, remove)
    }
    suspend fun sendMessage(text: String) {
        try {
            if (userId == null) return
            val isMemberOfChat = _uiState.value.isMemberOfChat
            if (isMemberOfChat != null && !isMemberOfChat) {
                chatServiceImpl.joinChat(chatId)
            }
            val currUsername = _uiState.value.currUsername
            if (currUsername != null) {
                val timestamp = chatServiceImpl.sendMessage(chatId, currUsername, text)
                chatServiceImpl.updateLastMessage(chatId, _uiState.value.currUsername,
                    _uiState.value.otherUsername,
                    _uiState.value.chat.copy(lastMessage = text, timestamp = timestamp))
            }
        } catch (e: Exception) {
            updateErrorMessage(e.message!!)
        }
    }
    suspend fun editMessage(message: Map<String, Message>) {
        try {
            chatServiceImpl.editMessage(chatId, message)
            if (_uiState.value.messages.entries.last().key == message.keys.first()) {
                chatServiceImpl.updateLastMessage(
                    chatId, _uiState.value.currUsername,
                    _uiState.value.otherUsername,
                    _uiState.value.chat.copy(lastMessage = message.values.first().text)
                )
            }
        } catch (e: Exception) {
            updateErrorMessage(e.message!!)
        }
    }
     suspend fun deleteMessage(message: Map<String, Message>) {
        try {
            val messages = _uiState.value.messages
            val entries = messages.entries
            chatServiceImpl.deleteMessage(chatId, message)
            if (entries.isNotEmpty() && entries.last().key == message.keys.first()) {
                val prevMessage = messages.values.toList()[messages.values.toList()
                    .indexOf(message.values.first())-1]
                val chat = _uiState.value.chat
                chatServiceImpl.updateLastMessage(chatId, _uiState.value.currUsername,
                    _uiState.value.otherUsername,
                    chat.copy(prevMessage.sender, lastMessage = prevMessage.text,
                        timestamp = prevMessage.timestamp))
            } else if (_uiState.value.messages.isEmpty()) {
                chatServiceImpl.updateLastMessage(chatId,_uiState.value.currUsername,
                    _uiState.value.otherUsername,
                    _uiState.value.chat.copy(lastMessage = ""))
                chatServiceImpl.leaveChat(chatId)
            }
        } catch (e: Exception) {
            updateErrorMessage(e.message!!)
        }
    }
    suspend fun scrollToBottom(listState: LazyListState, firstVisibleItem: LazyListItemInfo?) {
        if (firstVisibleItem != null) {
            chatServiceImpl.scrollToBottom(uiState.value.messages, listState, firstVisibleItem)
        }
    }
    fun formatMessageDate(timestamp: Long): String {
        return chatServiceImpl.formatDate(timestamp)
    }
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.formatStorageErrorMessage()) }
    }
    private fun updateErrorMessage(m: String) {
        if (Firebase.auth.currentUser != null) {
            _uiState.update { it.copy(errorMessage = m) }
        }
    }

    data class PrivateChatUiState(
        val messages: Map<String, Message> = mapOf(),
        val chat: Chat = Chat(),
        val isMemberOfChat: Boolean? = null,
        val topMessageBatchIndex: Int = 0,
        val picId: String = "",
        val currUsername: String? = "",
        val otherUsername: String? = "",
        val storageErrorMessage: String = "",
        val messagesState: MessagesState = MessagesState.IDLE,
        val errorMessage: String = "",
    )
}