package com.example.myspeechy.services

import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.UUID

private val database = Firebase.database.reference
interface ChatService {
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    val membersRef: DatabaseReference
        get() = database.child("members")
    val messagesRef: DatabaseReference
        get() = database.child("messages")
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
        onDataReceived: (DataSnapshot) -> Unit) {}

    fun messagesListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (List<DataSnapshot>) -> Unit) {
        messagesRef.child(id)
            .orderByChild("timestamp")
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun chatMembersListener(id: String,
                            onCancelled: (Int) -> Unit,
                            onDataReceived: (List<DataSnapshot>) -> Unit) {}
    fun sendMessage(chatId: String, chatTitle: String, text: String): Long {
        val timestamp = System.currentTimeMillis()
        messagesRef.child(chatId)
            .child(UUID.randomUUID().toString()).setValue(Message(userId, text, timestamp))
        return timestamp
    }

    fun updateLastMessage(chatId: String,chat: Chat) {}
    fun joinChat(chatId: String) {}

}

class PublicChatServiceImpl: ChatService {
    private val chatsRef: DatabaseReference
        get() = database.child("public_chats")
    override fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit
    ) {
        chatsRef.child(id)
            .addValueEventListener(chatEventListener(onCancelled, onDataReceived))
    }

    override fun chatMembersListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (List<DataSnapshot>) -> Unit
    ) {
        membersRef
            .child(id)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }

    override fun updateLastMessage(chatId: String, chat: Chat) {
        val publicChat = chat.copy(type = "public")
        chatsRef.child(chatId)
            .setValue(publicChat)
    }

    override fun joinChat(chatId: String) {
        membersRef.child(chatId).setValue(mapOf(userId to true))
    }

}

class PrivateChatServiceImpl: ChatService {
    private val chatsRef: DatabaseReference
        get() = database.child("private_chats")
    override fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit
    ) {
        chatsRef.child(id)
            .addValueEventListener(chatEventListener(onCancelled, onDataReceived))
    }

    override fun updateLastMessage(chatId: String, chat: Chat) {
        val userIds = chatId.split("_")
        val privateChat = chat.copy(type = "private")
         database.child("users").child(userIds[1]).child("name").get().addOnSuccessListener { username ->
             chatsRef.child(userIds[0])
                 .child(chatId)
                 .setValue(privateChat.copy(title = username.getValue<String>() ?: ""))
         }
        database.child("users").child(userIds[0]).child("name").get().addOnSuccessListener { username ->
            chatsRef.child(userIds[1])
                .child(chatId)
                .setValue(privateChat.copy(title = username.getValue<String>() ?: ""))
        }
    }

    override fun joinChat(chatId: String) {
        val userIds = chatId.split("_")
        chatsRef.child(userIds[0])
            .child(chatId)
            .setValue(Chat())
        chatsRef.child(userIds[1])
            .child(chatId)
            .setValue(Chat())
    }
}