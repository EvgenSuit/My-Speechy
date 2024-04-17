package com.example.myspeechy.data.chat

import androidx.annotation.Keep

@Keep
data class Chat(
    val title: String = "",
    val description: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val type: String = ""
)