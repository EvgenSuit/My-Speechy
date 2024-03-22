package com.example.myspeechy.data.chat

data class Message(
    var sender: String = "",
    var senderUsername: String? = null,
    val text: String = "",
    val timestamp: Long = 0,
    val edited: Boolean = false
)
