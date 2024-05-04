package com.myspeechy.myspeechy

import com.myspeechy.myspeechy.data.chat.Message
import com.myspeechy.myspeechy.domain.chat.RootChatService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.properties.Delegates


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
    private val messageSlot = slot<Message>()
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
            every { child("messages").child(chatId).child(any<String>()).setValue(capture(messageSlot)) } returns mockTask()
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
        assertTrue(!messageSlot.isCaptured)
    }
    @Test
    fun sendMessage_userIsNotNull_sendingPerformed() {
        runBlocking {
            rootChildService.sendMessage(chatId, username, text)
        }
        assertTrue(messageSlot.isCaptured)
        verify { mockedRdb.child("messages").child(chatId).child(any<String>()).setValue(messageSlot.captured) }
    }
}