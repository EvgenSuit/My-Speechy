package com.example.myspeechy.utils.chat

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
    init {
        chatsService.membershipListener({}) {membership ->
            if (membership.value != null) {
                (membership.value as Map<String, Map<String, String>>).keys.forEach { chatId ->
                    chatsService.chatsListener(chatId, {}) {chat ->
                    _uiState.update {
                        it.copy(chats = it.chats + mapOf(chatId to chat.getValue<Chat>()?.copy(type = "public")))
                    }
                }
                }
            }
        }
    listenForPrivateChats()
    }
    fun getChatPic(otherUserId: String): File = privateChatServiceImpl.getChatPic(filesDir.path, otherUserId)
    private fun listenForPrivateChats() {
        chatsService.privateChatsListener({}) {chats ->
            val chatsMap = chats.getValue<Map<String, Chat>>()
            chatsMap?.keys?.forEach { key ->
                val otherUserId = key.split("_").first { it != userId }
                privateChatServiceImpl.chatProfilePictureListener(otherUserId, filesDir.path, {}, { m ->
                    _uiState.update { it.copy(storageErrorMessage = m) } }) {
                    _uiState.update { it.copy(storageErrorMessage = "") }
                }
                _uiState.update {
                    it.copy(chats = it.chats + mapOf(key to chatsMap[key]?.copy(type = "private")))
                }
            }
        }
    }

    fun searchForChat(title: String) {
        chatsService.searchChatByTitle(title, {}) {chat ->
            if (chat.value != null) {
                val chatMap = chat.getValue<Map<String, Chat>>()
                val key = chatMap!!.keys.first()
                _uiState.update {
                    it.copy(searchedChat = mapOf(
                        key to chatMap[key]))
                }
            } else {
                _uiState.update {
                    ChatsUiState(chats = it.chats)
                }
            }
        }
    }

    data class ChatsUiState(
        val searchedChat: Map<String?, Chat?> = mapOf(),
        val chats: Map<String?, Chat?> = mapOf(),
        val storageErrorMessage: String = ""
    )
}