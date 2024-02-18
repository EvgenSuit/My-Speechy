package com.example.myspeechy.services.chat

import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private val database = Firebase.database.reference
interface ChatsService {
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    fun listener(
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
    fun searchChatByTitle(title: String,
                          onCancelled: (Int) -> Unit,
                          onDataReceived: (DataSnapshot) -> Unit) {
        database.child("public_chats")
            .orderByChild("title")
            .equalTo(title)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun membershipListener(onCancelled: (Int) -> Unit,
                           onDataReceived: (DataSnapshot) -> Unit) {
        database.child("members")
            .orderByChild(userId)
            .equalTo(true)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun privateChatsListener(onCancelled: (Int) -> Unit,
                             onDataReceived: (DataSnapshot) -> Unit) {
        database.child("private_chats")
            .child(userId)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun chatsListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit) {
        database.child("public_chats")
            .child(id)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
}
class ChatsServiceImpl: ChatsService