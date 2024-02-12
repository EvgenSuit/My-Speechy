package com.example.myspeechy.services

import android.util.Log
import com.example.myspeechy.data.Chat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private val database = Firebase.database.reference
private interface ChatsService {

    fun joinChat() {

    }
    fun listener(
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataReceived(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.d("ERROR", error.toString())
                onCancelled(error.code)
            }
        }
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