package com.example.myspeechy.useCases

import com.example.myspeechy.utils.chat.getOtherUserId
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val database = Firebase.database.reference
private val userId = Firebase.auth.currentUser!!.uid
class LeavePrivateChatUseCase {
    suspend operator fun invoke(chatId: String) {
        val otherUserId = chatId.getOtherUserId(userId)
        database.child("users")
            .child(otherUserId)
            .child("private_chats")
            .child(chatId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //remove messages if the other user has already deleted the chat for themselves
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
            .removeValue().await()
        database.child("private_chats")
                .child(userId)
                .child(chatId)
                .removeValue().await()
    }
}

class LeavePublicChatUseCase {
    suspend operator fun invoke(chatId: String) {
        database.child("members")
            .child(chatId)
            .child(userId)
            .removeValue().await()
        database.child("users")
            .child(userId)
            .child("public_chats")
            .child(chatId)
            .removeValue().await()
    }
}

class JoinPublicChatUseCase {
    operator fun invoke(chatId: String) {
        database.child("members")
            .child(chatId)
            .child(userId).setValue(true)
        database.child("users")
            .child(userId)
            .child("public_chats")
            .child(chatId)
            .setValue(true)
    }
}
class DeletePublicChatUseCase {
    suspend operator fun invoke(chatId: String) {
        removeChat(chatId)
        removeMessages(chatId)
        removeMembers(chatId)
        revokeAdminPermissions(chatId)
    }
    private suspend fun revokeAdminPermissions(chatId: String) {
        database.child("admins")
            .child(chatId)
            .removeValue().await()
    }
    private suspend fun removeMembers(chatId: String) {
        database.child("members")
            .child(chatId)
            .removeValue().await()
    }
    private suspend fun removeMessages(chatId: String) {
        database.child("messages")
            .child(chatId)
            .removeValue().await()
    }
    private suspend fun removeChat(chatId: String) {
        database.child("public_chats")
            .child(chatId)
            .removeValue()
            .await()
    }
}
class CheckIfIsAdminUseCase {
    suspend operator fun invoke(chatId: String, onReceived: (Boolean) -> Unit) {
        val admin = database.child("admins")
            .child(chatId)
            .get().await()
        onReceived(admin.getValue<String>() == userId)
    }
}

