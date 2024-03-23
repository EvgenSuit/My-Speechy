package com.example.myspeechy.utils.chat

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.data.chat.MessagesState
import com.example.myspeechy.services.chat.PublicChatServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PublicChatViewModel @Inject constructor(
    private val chatServiceImpl: PublicChatServiceImpl,
    private val filesDirPath: String,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val _uiState = MutableStateFlow(PublicChatUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatServiceImpl.userId

    fun startOrStopListening(removeListeners: Boolean) {
        checkIfIsMemberOfChat(removeListeners)
        listenForAdmin(removeListeners)
        listenForCurrentChat(removeListeners)
        checkIfChatIsEmpty(removeListeners)
        listenForChatMembers(removeListeners)
        if (removeListeners) listenForMessages(remove = true)
    }
    private fun checkIfChatIsEmpty(remove: Boolean) {
        chatServiceImpl.checkIfChatIsEmpty(chatId, remove) {isEmpty ->
            _uiState.update { it.copy(messagesState = if (isEmpty) MessagesState.EMPTY else MessagesState.IDLE) }
        }
    }
    private fun checkIfIsMemberOfChat(remove: Boolean) {
        chatServiceImpl.checkIfIsMemberOfChat(chatId, remove) {isMember ->
            _uiState.update { it.copy(joined = isMember) }
        }
    }

    fun handleDynamicMessageLoading(loadOnResume: Boolean = false,
                                    lastVisibleItemIndex: Int?) {
        chatServiceImpl.handleDynamicMessageLoading(
            loadOnResume = loadOnResume,
            topMessageIndex = _uiState.value.topMessageBatchIndex,
            lastVisibleItemIndex = lastVisibleItemIndex,
            onRemove = {listenForMessages(remove = true)},
            onLoad = {listenForMessages(it, false)})
    }

    private fun listenForMessages(topIndex: Int = 0, remove: Boolean) {
        if (!remove) {
            _uiState.update { it.copy(topMessageBatchIndex = it.topMessageBatchIndex + topIndex) }
        }
        if (_uiState.value.messagesState != MessagesState.EMPTY) {
            _uiState.update { it.copy(messagesState = MessagesState.LOADING) }
            chatServiceImpl.messagesListener(
                chatId, _uiState.value.topMessageBatchIndex,
                onAdded = { m ->
                    val id = m.keys.first()
                    val savedMessage = _uiState.value.messages[id]
                    val message = m.values.first()
                    if (savedMessage != null) {
                        _uiState.update {
                            it.copy(
                                it.messages + mapOf(id to message
                                        .copy(senderUsername = savedMessage.senderUsername)))
                        }
                    } else {
                        val newMessages = _uiState.value.messages + m
                        _uiState.update {
                            it.copy(newMessages.toSortedMap(compareBy { k -> newMessages[k]?.timestamp }))
                        }
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
                onCancelled = {},
                remove
            )
        }
    }
    /*fun handleDynamicMembersLoading(loadOnResume: Boolean = false, lastVisibleItemIndex: Int?) {
        chatServiceImpl.handleDynamicMembersLoading(loadOnResume,
            maxMemberIndex = _uiState.value.lastMemberBatchIndex,
            lastVisibleItemIndex,
            onRemove = {listenForChatMembers(remove = true)},
            onLoad = {lastIndex -> listenForChatMembers(lastIndex, false)})
    }*/
    private fun listenForChatMembers(remove: Boolean) {
        if (remove) {
            _uiState.value.members.forEach {
                chatServiceImpl.usernameListener(it.key, {}, {}, true ) }
        }
        chatServiceImpl.chatMembersListener(chatId,
            onAdded = {m ->
                _uiState.update { it.copy(members = it.members + mapOf(m.keys.first() to "")) }
                m.keys.forEach { userId ->
                    listenForUsername(userId, remove)
                    listenForProfilePic(userId, remove)
                }
            },
            onChanged = { },
            onRemoved = {m ->
                _uiState.update { it.copy(members = it.members.filterKeys { key -> key != m.keys.first() }) }
                m.keys.forEach {userId ->
                    listenForUsername(userId, true)
                    listenForProfilePic(userId, true)
                }
            },
            onCancelled = {},
            remove)
    }
    private fun listenForProfilePic(id: String, remove: Boolean) {
            val picDir = "${filesDirPath}/profilePics/${id}/"
            val picPath = "$picDir/lowQuality/$id.jpg"
            chatServiceImpl.usersProfilePicListener(id, filesDirPath, {}, {updateStorageErrorMessage(it)
                File(picDir).deleteRecursively() }, {
                _uiState.update { it.copy(picPaths = it.picPaths.toMutableMap().apply { this[id] = picPath },
                    picsRecomposeIds = it.picsRecomposeIds.toMutableMap().apply { this[id] = UUID.randomUUID().toString() }) }
            }, remove)
    }
    private fun listenForUsername(id: String, remove: Boolean) {
        chatServiceImpl.usernameListener(id, {}, {username ->
            val name = username.getValue<String>()
            _uiState.update { it.copy(members =
                it.members.mapValues { (k, v) -> if (k == id && name != null) name else v},
            //else it.members.filterKeys { k -> k == id },

                messages = it.messages.mapValues { (_, v) ->
                    if (v.sender == id && v.senderUsername != name) v.copy(senderUsername = name) else v }) }
        }, remove)
    }
    private fun listenForCurrentChat(remove: Boolean) {
            chatServiceImpl.chatListener(chatId, {}, { chat ->
                _uiState.update {
                    it.copy(chat = chat.getValue<Chat>() ?: Chat(), chatLoaded = true)
                }
            }, remove)
    }
    private fun listenForAdmin(remove: Boolean) {
        chatServiceImpl.listenForAdmin(chatId, {}, {snapshot ->
            val value = snapshot.getValue<String>()
             _uiState.update { it.copy(isAdmin = (value == userId), admin = value) }
        }, remove)
    }
    suspend fun sendMessage(text: String) {
        val chat = _uiState.value.chat
        val timestamp = chatServiceImpl.sendMessage(chatId, _uiState.value.members.entries.first { it.key == userId }.value, text)
        chatServiceImpl.updateLastMessage(chatId, chat.copy(lastMessage = text, timestamp = timestamp))
    }
    suspend fun editMessage(message: Map<String, Message>) {
        chatServiceImpl.editMessage(chatId, message)
        if (_uiState.value.messages.entries.last().key == message.keys.first()) {
            chatServiceImpl.updateLastMessage(chatId,
                _uiState.value.chat.copy(lastMessage = message.values.first().text))
        }
    }
     suspend fun deleteMessage(message: Map<String, Message>) {
         chatServiceImpl.deleteMessage(chatId, message)
        val messages = _uiState.value.messages
        val entries = messages.entries
        if (entries.isNotEmpty() && entries.last().value == message.values.first()) {
            val prevMessage = messages.values.toList()[messages.values.toList()
                .indexOf(message.values.first()) - 1]
            chatServiceImpl.updateLastMessage(
                chatId, _uiState.value.chat.copy(
                    lastMessage = prevMessage.text,
                    timestamp = prevMessage.timestamp
                )
            )
        } else if (_uiState.value.messages.isEmpty()) {
            chatServiceImpl.updateLastMessage(chatId, _uiState.value.chat.copy(lastMessage = ""))
        }
    }

    fun changeChat(title: String, description: String) {
        chatServiceImpl.changePublicChat(chatId, _uiState.value.chat.copy(title = title, description = description))
    }
    suspend fun scrollToBottom(listState: LazyListState, firstVisibleItem: LazyListItemInfo?) {
        if (firstVisibleItem != null) {
            chatServiceImpl.scrollToBottom(uiState.value.messages, listState, firstVisibleItem)
        }
    }
    fun formatMessageDate(timestamp: Long): String {
        return chatServiceImpl.formatDate(timestamp)
    }

    fun joinChat() {
        chatServiceImpl.joinChat(chatId)
    }
    private fun updateStorageErrorMessage(e: String) {
        _uiState.update { it.copy(storageErrorMessage = e.formatStorageErrorMessage()) }
    }

    data class PublicChatUiState(
        val messages: Map<String, Message> = mapOf(),
        val chat: Chat = Chat(),
        val topMessageBatchIndex: Int = 0,
        val lastMemberBatchIndex: Int = 0,
        val chatLoaded: Boolean = false,
        val members: Map<String, String> = mapOf(), //UserId to username map
        val errorCode: Int = 0,
        val joined: Boolean = false,
        val isAdmin: Boolean = false,
        val admin: String? = "",
        val storageErrorMessage: String = "",
        val messagesState: MessagesState = MessagesState.IDLE,
        val picPaths: Map<String, String> = mapOf(), //user id to pic path map
        val picsRecomposeIds: Map<String, String> = mapOf()
    )
}