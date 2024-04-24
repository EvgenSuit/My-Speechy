package com.example.myspeechy

import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.domain.chat.RootChatService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID


private class FakeRootChildService(
    override val userId: String?,
    override val messagesRef: DatabaseReference,
    override val picsRef: StorageReference? = null,
    override val usersRef: DatabaseReference? = null
) : RootChatService {
    override fun chatListener(
        id: String,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun messagesListener(
        id: String,
        topIndex: Int,
        onAdded: (Map<String, Message>) -> Unit,
        onChanged: (Map<String, Message>) -> Unit,
        onRemoved: (Map<String, Message>) -> Unit,
        onCancelled: (String) -> Unit,
        remove: Boolean
    ) {}


    override fun usernameListener(
        id: String?,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean
    ) {}
}

class RootChatServiceUnitTests {
    private val chatId: String = "chatId"
    private val messageId: String = UUID.randomUUID().toString()
    private val timestamp: Long = OffsetDateTime.now(ZoneOffset.UTC).toZonedDateTime().toInstant().toEpochMilli()
    private val text: String = "message text"
    private lateinit var mockedRdb: DatabaseReference
    private lateinit var rootChildService: FakeRootChildService

    @Before
    fun mockDependencies() {
        mockRdb()
        initRootChatService()
    }

    private fun mockRdb() {
        mockedRdb = mockk<DatabaseReference> {
            every { child("messages").child(chatId).child(messageId).setValue(
                Message(messageId, username, text, timestamp)) } returns mockTask()
        }
    }
    private fun initRootChatService() {
        rootChildService = FakeRootChildService(userId, mockedRdb.child("messages"))
    }

    @Test
    fun sendMessage_userIsNull_sendingFailed() {
        rootChildService = FakeRootChildService(null, mockedRdb.child("messages"))
        runBlocking {
            rootChildService.sendMessage(chatId, username, text)
        }
        verify(exactly = 0) { mockedRdb.child(chatId).child(messageId).setValue(
            Message(messageId, username, text, timestamp, false)) }
    }
    @Test
    fun sendMessage_userIsNotNull_sendingPerformed() {
        runBlocking {
            rootChildService.sendMessage(chatId, username, text)
        }
        verify { mockedRdb.child(chatId).child(messageId).setValue(
            Message(messageId, username, text, timestamp, false)) }
    }
}