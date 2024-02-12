package com.example.myspeechy.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.myspeechy.data.Chat
import com.example.myspeechy.services.ChatsServiceImpl
import com.google.firebase.database.getValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatsService: ChatsServiceImpl
): ViewModel() {
    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()
    fun listen() {
        val id = "0"
        chatsService.chatsListener(id,{
                _uiState.update {
                    it.copy(chats = mapOf(id to null))
                }
        }) { chat ->
            _uiState.update {
                it.copy(chats = mapOf(id to chat.getValue<Chat>()))
            }
            chat.getValue<Chat>()
            Log.d("CHAT", chat.value.toString())
        }
    }
    init {
        //todo Add to common group chat
        listen()
    }

    data class ChatsUiState(val chats: Map<String, Chat?> = mapOf())
}