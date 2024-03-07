package com.example.myspeechy.utils.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.services.chat.PublicChatServiceImpl
import com.example.myspeechy.useCases.GetProfileOrChatPictureUseCase
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PublicChatViewModel @Inject constructor(
    private val chatServiceImpl: PublicChatServiceImpl,
    private val filesDirPath: String,
    @Named("ChatDataStore") private val chatDataStore: ChatDatastore,
    private val getProfileOrChatPictureUseCase: GetProfileOrChatPictureUseCase,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val _uiState = MutableStateFlow(PublicChatUiState())
    val uiState = _uiState.asStateFlow()
    val userId = chatServiceImpl.userId
    init {
        //immediately check if user has joined the chat
        runBlocking {
            val joined = chatDataStore.checkState(chatId)
            if (joined != null) {
                _uiState.update { it.copy(joined = joined) }
            }
        }
        viewModelScope.launch {
            chatDataStore.collectState(chatId) {joined ->
                _uiState.update { it.copy(joined = joined) }
            }
        }
    }
    fun startOrStopListening(removeListeners: Boolean) {
        listenForCurrentChat(removeListeners)
        listenForAdmin(removeListeners)
        listenForChatMembers(removeListeners)
        listenForMessages(removeListeners)
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
                _uiState.update { it.copy(members = it.members + mapOf(m.keys.first() to "")) }
                m.keys.forEach { userId ->
                    listenForUsername(userId, remove)
                    listenForProfilePic(userId, remove)
                }
                if ((_uiState.value.members + m).containsKey(userId) && !uiState.value.joined) {
                    viewModelScope.launch {
                        chatDataStore.addToChatList(chatId)
                    }
                }
            },
            onChanged = { },
            onRemoved = {m ->
                _uiState.update { it.copy(members = it.members.filterKeys { key -> key != m.keys.first() }) }
                if (!_uiState.value.members.containsKey(userId)) {
                    viewModelScope.launch {
                        chatDataStore.removeFromChatList(chatId)
                    }
                }
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
            if (name != null) {
                _uiState.update { it.copy(members = it.members.mapValues { (k, v) -> if (k == id) name else v},
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
    private fun listenForAdmin(remove: Boolean) {
        chatServiceImpl.listenForAdmin(chatId, {}, {snapshot ->
             _uiState.update { it.copy(isAdmin = (snapshot.exists() && snapshot.value == userId)) }
        }, remove)
    }

        fun sendMessage(text: String, replyTo: String) {
            val chat = _uiState.value.chat
            val timestamp = chatServiceImpl.sendMessage(chatId, _uiState.value.members.entries.first { it.key == userId }.value, text, replyTo)
            chatServiceImpl.updateLastMessage(chatId, chat.copy(lastMessage = text, timestamp = timestamp))
        }
    fun editMessage(message: Map<String, Message>) {
        chatServiceImpl.editMessage(chatId, message)
        chatServiceImpl.updateLastMessage(chatId,
            _uiState.value.chat.copy(lastMessage = message.values.first().text))
    }
    fun deleteMessage(message: Map<String, Message>) {
        val messages = _uiState.value.messages
        val entries = messages.entries
        chatServiceImpl.deleteMessage(chatId, message)
        if (entries.last().value == message.values.first() && entries.size > 1) {
            val prevMessage = messages.values.toList()[messages.values.toList()
                .indexOf(message.values.first()) - 1]
            chatServiceImpl.updateLastMessage(
                chatId, _uiState.value.chat.copy(
                    lastMessage = prevMessage.text,
                    timestamp = prevMessage.timestamp
                )
            )
        } else if (entries.size <= 1) {
            chatServiceImpl.updateLastMessage(chatId, Chat(_uiState.value.chat.title)) {
                viewModelScope.launch {
                    chatServiceImpl.leaveChat(chatId)
                }
            }
        }
    }

    fun changeChat(title: String, description: String) {
        chatServiceImpl.changePublicChat(chatId, _uiState.value.chat.copy(title = title, description = description))
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
        val members: Map<String, String> = mapOf(), //UserId to username map
        val errorCode: Int = 0,
        val joined: Boolean = false,
        val isAdmin: Boolean = false,
        val storageErrorMessage: String = "",
        val picPaths: Map<String, String> = mapOf(), //user id to pic path map
        val picsRecomposeIds: Map<String, String> = mapOf()
    )
}