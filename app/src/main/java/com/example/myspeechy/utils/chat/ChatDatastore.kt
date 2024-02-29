package com.example.myspeechy.utils.chat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

private val Context.chatDatastore: DataStore<Preferences> by preferencesDataStore("Chat")
class ChatDatastore(val context: Context) {
    private val publicJoinedChatsKey = stringSetPreferencesKey("publicJoinedChats")
    suspend fun checkState(chatId: String): Boolean? = context.chatDatastore.data.first()[publicJoinedChatsKey]?.contains(chatId)
    suspend fun collectState(chatId: String, onCollected: (Boolean) -> Unit) {
        context.chatDatastore.data.collectLatest { preferences ->
            val currState = preferences[publicJoinedChatsKey]
            if (currState != null) {
                onCollected(currState.contains(chatId))
            }
        }
    }

    suspend fun addToChatList(chatId: String) {
        context.chatDatastore.edit { preferences ->
            preferences[publicJoinedChatsKey] = (preferences[publicJoinedChatsKey] ?: setOf()) + chatId
        }
    }
    suspend fun removeFromChatList(chatId: String) {
        context.chatDatastore.edit { preferences ->
            preferences[publicJoinedChatsKey] = (preferences[publicJoinedChatsKey] ?: setOf()) - chatId
        }
    }
}