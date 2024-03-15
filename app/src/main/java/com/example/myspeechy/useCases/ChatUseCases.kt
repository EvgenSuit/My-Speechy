package com.example.myspeechy.useCases

import com.example.myspeechy.utils.chat.getOtherUserId
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
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

class LeavePublicChatUseCase {
    operator fun invoke(chatId: String) {
        database.child("members")
            .child(chatId)
            .child(userId)
            .removeValue()
        database.child("users")
            .child(userId)
            .child("public_chats")
            .child(chatId)
            .removeValue()
    }
}

class JoinPublicChatUseCase {
    operator fun invoke(chatId: String, onSuccess: () -> Unit) {
        database.child("members")
            .child(chatId)
            .child(userId).setValue(true)
        database.child("users")
            .child(userId)
            .child("public_chats")
            .child(chatId)
            .setValue(true)
            .addOnSuccessListener { onSuccess() }
    }
}
class DeletePublicChatUseCase {
    operator fun invoke(chatId: String, onDeleted: () -> Unit) {
        removeChat(chatId) {
            removeMessages(chatId) {
                removeMembers(chatId) {
                    revokeAdminPermissions(chatId) { onDeleted() }
                }
            }
        }
    }
    private fun revokeAdminPermissions(chatId: String, onRevoked: () -> Unit) {
        database.child("admins")
            .child(chatId)
            .removeValue().addOnSuccessListener { onRevoked() }
    }
    private fun removeMembers(chatId: String, onRemoved: () -> Unit) {
        database.child("members")
            .child(chatId)
            .removeValue().addOnSuccessListener { onRemoved() }
    }
    private fun removeMessages(chatId: String, onRemoved: () -> Unit) {
        database.child("messages")
            .child(chatId)
            .removeValue().addOnSuccessListener { onRemoved() }
    }
    private fun removeChat(chatId: String, onRemoved: () -> Unit) {
        database.child("public_chats")
            .child(chatId)
            .removeValue()
            .addOnSuccessListener { onRemoved() }
    }
}
class CheckIfIsAdminUseCase {
    operator fun invoke(chatId: String, onReceived: (Boolean) -> Unit) {
        database.child("admins")
            .child(chatId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isAdmin = snapshot.getValue<String>() == userId
                    onReceived(isAdmin)
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
}

class GetProfileOrChatPictureUseCase(private val filesDir: String) {
    private fun getPicDir(otherUserId: String): String
            = "${filesDir}/profilePics/$otherUserId/lowQuality"
}
