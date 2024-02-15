package com.example.myspeechy.data.chat

data class Message(
    val sender: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val timestamp: Long = 0
)
