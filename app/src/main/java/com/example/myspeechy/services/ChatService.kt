package com.example.myspeechy.services

import android.util.Log
import com.example.myspeechy.data.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.UUID

private val database = Firebase.database.reference
private interface ChatService {
    val ref: DatabaseReference
        get() = database.child("messages")
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    fun listener(
        onCancelled: (Int) -> Unit,
        onDataReceived: (List<DataSnapshot>) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataReceived(snapshot.children.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
        }
    }
    fun chatEventListener(
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataReceived(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
        }
    }
    fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit
    ) {
        database.child("chats")
            .child(id)
            .addValueEventListener(chatEventListener(onCancelled, onDataReceived))
    }
    fun messagesListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (List<DataSnapshot>) -> Unit) {
        ref.child(id)
            .orderByChild("timestamp")
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }

    fun updateLastMessage(chatId: String, title: String, text: String, timestamp: Long) {
        database.child("chats")
            .child(chatId).setValue(mapOf(
                "lastMessage" to text,
                "timestamp" to timestamp,
                "title" to title
                ))
    }
    fun listenChatMembers(id: String,
                          onCancelled: (Int) -> Unit,
                          onDataReceived: (List<DataSnapshot>) -> Unit) {
        database.child("members")
            .child(id)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun joinChat(chatId: String) {
        database.child("members")
            .child(chatId)
            .setValue(mapOf("user_id" to userId))
    }

    fun sendMessage(chatId: String, chatTitle: String, text: String) {
        val timestamp = System.currentTimeMillis()
        ref.child(chatId)
            .child(UUID.randomUUID().toString()).setValue(Message(userId, text, timestamp))
        updateLastMessage(chatId, chatTitle, text, timestamp)
    }
}

class ChatServiceImpl: ChatService