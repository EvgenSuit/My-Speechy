package com.example.myspeechy.services

import android.util.Log
import com.example.myspeechy.data.Chat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private val database = Firebase.database.reference
private interface ChatsService {
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
        database.child("chats")
            .orderByChild("title")
            .equalTo(title)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun membershipListener(onCancelled: (Int) -> Unit,
                           onDataReceived: (DataSnapshot) -> Unit) {
        database.child("members")
            .orderByChild("user_id")
            .equalTo(userId)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun chatsListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit) {
        database.child("chats")
            .child(id)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
}
class ChatsServiceImpl: ChatsService