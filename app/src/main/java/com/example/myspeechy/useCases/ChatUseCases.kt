package com.example.myspeechy.useCases

import android.util.Log
import com.example.myspeechy.presentation.chat.getOtherUserId
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.getValue
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class LeavePrivateChatUseCase(
    private val userId: String?,
    private val database: DatabaseReference
) {
    suspend operator fun invoke(chatId: String) {
        if (userId == null) return
        val otherUserId = chatId.getOtherUserId(userId)
        val snapshot = database.child("users")
            .child(otherUserId)
            .child("private_chats")
            .child(chatId)
            .get().await()
        //remove messages if the other user has already left the chat
        if (!snapshot.exists()) {
            database.child("messages")
                .child(chatId)
                .removeValue().await()
        }
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

class LeavePublicChatUseCase(private val userId: String?,
                             private val database: DatabaseReference,
    private val decrementMemberCountUseCase: DecrementMemberCountUseCase) {
    suspend operator fun invoke(chatId: String, revokeMembership: Boolean = true) {
        if (userId == null) return
        if (revokeMembership) {
            decrementMemberCountUseCase(chatId)
            database.child("members")
                .child(chatId)
                .child(userId)
                .removeValue().await()
        }
        database.child("users")
            .child(userId)
            .child("public_chats")
            .child(chatId)
            .removeValue().await()
    }
}

class JoinPublicChatUseCase(
    private val userId: String?,
    private val database: DatabaseReference) {
    suspend operator fun invoke(chatId: String) {
        if (userId == null) return
        database.child("members")
            .child(chatId)
            .child(userId).setValue(true).await()
        incrementMemberCount(chatId)
        database.child("users")
            .child(userId)
            .child("public_chats")
            .child(chatId)
            .setValue(true).await()
    }
    private suspend fun incrementMemberCount(chatId: String) {
        val ref = database.child("member_count").child(chatId)
        val memberCount = ref.get().await().getValue(Int::class.java) ?: 0
        ref.setValue(memberCount+1).await()
    }
}
class DecrementMemberCountUseCase(private val database: DatabaseReference) {
    suspend operator fun invoke(chatId: String, remove: Boolean = false) {
        val ref = database.child("member_count").child(chatId)
        val memberCount = ref.get().await().getValue(Int::class.java) ?: return
        if (memberCount-1 <= 0 || remove) ref.removeValue().await()
        else ref.setValue(memberCount-1).await()
    }
}
class DeletePublicChatUseCase(private val database: DatabaseReference,
    private val decrementMemberCountUseCase: DecrementMemberCountUseCase) {
    suspend operator fun invoke(chatId: String) {
        removeChat(chatId)
        removeMessages(chatId)
        decrementMemberCountUseCase(chatId, true)
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
            .removeValue().await()
    }
}
class CheckIfIsAdminUseCase(private val userId: String?,
                            private val database: DatabaseReference) {
    suspend operator fun invoke(chatId: String): Boolean {
        val admin = database.child("admins")
            .child(chatId)
            .get().await()
        return admin.getValue(String::class.java) == userId
    }
}

class FormatDateUseCase {
    operator fun invoke(timestamp: Long): String {
        var targetDateFormat = ""
        val currentDate = LocalDateTime.now()
        val messageDateFormatted = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        if (currentDate.year != messageDateFormatted.year) targetDateFormat += "yyyy."
        if (currentDate.dayOfMonth != messageDateFormatted.dayOfMonth) targetDateFormat += "MMM dd "
        targetDateFormat += "HH:mm"
        return SimpleDateFormat(targetDateFormat, Locale.getDefault()).format(timestamp)
    }
}

