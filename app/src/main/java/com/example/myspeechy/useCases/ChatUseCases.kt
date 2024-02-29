package com.example.myspeechy.useCases

import com.example.myspeechy.utils.chat.ChatDatastore
import com.example.myspeechy.utils.chat.getOtherUserId
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
private val database = Firebase.database.reference
private val userId = Firebase.auth.currentUser!!.uid
class LeavePrivateChatUseCase {
    operator fun invoke(chatId: String) {
        val otherUserId = chatId.getOtherUserId(userId)
        database.child("users")
            .child(otherUserId)
            .child("private_chats")
            .child(chatId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //remove messages if the other user has deleted the chat for themselves
                    if (!snapshot.exists()) {
                        database.child("messages")
                            .child(chatId)
                            .removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        database.child("users")
            .child(userId)
            .child("private_chats")
            .child(chatId)
            .removeValue()
        database.child("private_chats")
                .child(userId)
                .child(chatId)
                .removeValue()
    }
}

class LeavePublicChatUseCase(private val chatDatastore: ChatDatastore) {
    suspend operator fun invoke(chatId: String) {
        database.child("members")
            .child(chatId)
            .child(userId)
            .removeValue()
        database.child("users")
            .child(userId)
            .child("public_chats")
            .child(chatId)
            .removeValue()
        chatDatastore.removeFromChatList(chatId)
    }
}