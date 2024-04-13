package com.example.myspeechy.data.chat

import androidx.annotation.Keep

@Keep
data class Message(
    var sender: String = "",
    var senderUsername: String? = null,
    val text: String = "",
    val timestamp: Long = 0,
    val edited: Boolean = false
)

enum class MessagesState {
    IDLE,
    LOADING,
    EMPTY,
}
enum class MembersState {
    IDLE,
    LOADING
}