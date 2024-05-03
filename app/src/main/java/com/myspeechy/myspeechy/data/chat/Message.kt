package com.myspeechy.myspeechy.data.chat

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
    NOT_EMPTY
}
enum class MembersState {
    IDLE,
    LOADING
}