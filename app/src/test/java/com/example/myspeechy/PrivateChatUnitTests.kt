package com.example.myspeechy

import com.example.myspeechy.presentation.chat.getOtherUserId
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PrivateChatUnitTests {
    private var mockedRdb = mockk<DatabaseReference>()
    val privateChats = listOf("fd38rfj_$userId", "dkfj_$userId")
    @Before
    fun mockDependencies() {
        mockRdb()
    }

    fun mockRdb() {
        val mockedPrivateChatsSnapshot = mockk<DataSnapshot> {
            every { children } returns privateChats.map { mockk<DataSnapshot> {
                every { key } returns it
            } }
        }
        val mockedOtherHasChat = mockk<DataSnapshot> {
            every { exists() } returns true
        }
        for (chatId in privateChats) {
            every { mockedRdb.child("users").child(chatId.getOtherUserId(userId)).child("private_chats").child(chatId).get() } returns mockTask(mockedOtherHasChat)
        }
        every { mockedRdb.child("users").child(userId).child("private_chats").get() } returns mockTask(mockedPrivateChatsSnapshot)
        for (chatId in privateChats) {
            every { mockedRdb.child("messages").child(chatId).removeValue() } returns mockTask()
            every { mockedRdb.child("users")
                .child(userId)
                .child("private_chats")
                .child(chatId)
                .removeValue() } returns mockTask()
            every { mockedRdb.child("private_chats")
                .child(userId)
                .child(chatId)
                .removeValue() } returns mockTask()
        }
    }

    @Test
    fun deleteChat_successOtherUserDoesNotHavePrivateChat_deletionPerformed() {
        val mockedOtherHasChat = mockk<DataSnapshot> {
            every { exists() } returns false
        }
        for (chatId in privateChats) {
            every { mockedRdb.child("users").child(chatId.getOtherUserId(userId)).child("private_chats").child(chatId).get() } returns mockTask(mockedOtherHasChat)
        }
        // TODO init view model

        for (chatId in privateChats) {
            verify { mockedRdb.child("users").child(chatId.getOtherUserId(userId)).child("private_chats").child(chatId).get() }
            verify { mockedRdb.child("messages").child(chatId).removeValue() }
            verify { mockedRdb.child("users")
                .child(userId)
                .child("private_chats")
                .child(chatId).removeValue() }
        }
    }
}