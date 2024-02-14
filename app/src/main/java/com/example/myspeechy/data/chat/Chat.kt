package com.example.myspeechy.data.chat

data class Chat(
    val title: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val type: String = ""
)