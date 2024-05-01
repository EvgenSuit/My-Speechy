package com.myspeechy.myspeechy

import androidx.lifecycle.SavedStateHandle
import com.myspeechy.myspeechy.data.chat.Chat
import com.myspeechy.myspeechy.domain.chat.PublicChatServiceImpl
import com.myspeechy.myspeechy.domain.useCases.DecrementMemberCountUseCase
import com.myspeechy.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.JoinPublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePublicChatUseCase
import com.myspeechy.myspeechy.presentation.chat.PublicChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class PublicChatUnitTests {
    val chatId = "djfjdkg"
    val filesDirPath = "path"
    val memberCount = 12
    private lateinit var mockedRdb: DatabaseReference
    private lateinit var mockedAuth: FirebaseAuth
    private var mockedStorage = mockk<StorageReference>()
    private lateinit var publicChatService: PublicChatServiceImpl
    private lateinit var viewModel: PublicChatViewModel
    @Before
    fun mockDependencies() {
        mockRdb()
        mockAuth()
        mockStorage(mockedStorage)
        publicChatService = initPublicChatService()
        val mockedSavedStateHandle = mockk<SavedStateHandle> {
            every { get<String>("chatId") } returns chatId
        }
        viewModel = PublicChatViewModel(publicChatService, filesDirPath, mockedSavedStateHandle)
    }
    fun mockAuth() {
        mockedAuth = mockk<FirebaseAuth> {
            every { currentUser } returns mockk<FirebaseUser>()
            every { currentUser?.uid } returns userId
        }
    }
    fun mockRdb() {
        mockedRdb = mockk<DatabaseReference>()
        val mockedPublicChatsSnapshot = mockk<DataSnapshot> {
            every { children } returns listOf(mockk<DataSnapshot> { every { key } returns chatId } )
        }
        every { mockedRdb.child("users").child(userId).child("public_chats").get() } returns mockTask(mockedPublicChatsSnapshot)
        //if at least one functionality in use cases doesn't get mocked and gets called,
        //the loop simply gets cancelled
        val mockedMemberCount = mockk<DataSnapshot> {
            every { getValue(Int::class.java) } returns memberCount
        }
            val decreasedMemberCount = mockedMemberCount.getValue(Int::class.java)
                ?.minus(1)
        val increasedMemberCount = mockedMemberCount.getValue(Int::class.java)
            ?.plus(1)
        every { mockedRdb.child("member_count").child(chatId).get() } returns mockTask(mockedMemberCount)
        every { mockedRdb.child("member_count").child(chatId).removeValue() } returns mockTask()
        every { mockedRdb.child("member_count").child(chatId).setValue(decreasedMemberCount) } returns mockTask()
        every { mockedRdb.child("member_count").child(chatId).setValue(increasedMemberCount) } returns mockTask()
        every { mockedRdb.child("messages").child(chatId).removeValue() } returns mockTask()
        every { mockedRdb.child("members").child(chatId).removeValue() } returns mockTask()
        every { mockedRdb.child("admins").child(chatId).removeValue() } returns mockTask()
        every { mockedRdb.child("members").child(chatId).child(userId).removeValue() } returns mockTask()
        every { mockedRdb.child("members").child(chatId).child(userId).setValue(true) } returns mockTask()
        every { mockedRdb.child("users").child(userId).child("public_chats").child(chatId).setValue(true) } returns mockTask()
        every { mockedRdb.child("users").child(userId).child("public_chats").child(chatId).removeValue() } returns mockTask()
        every { mockedRdb.child("public_chats").child(chatId).setValue(any()) } returns mockTask()
        every { mockedRdb.child("public_chats").child(chatId).removeValue() } returns mockTask()
    }
    fun initPublicChatService(): PublicChatServiceImpl {
        val decrementMemberCountUseCase = DecrementMemberCountUseCase(mockedRdb)
        val leavePublicChatUseCase = LeavePublicChatUseCase(userId, mockedRdb, decrementMemberCountUseCase)
        val deletePublicChatUseCase = DeletePublicChatUseCase(mockedRdb, decrementMemberCountUseCase)
        val joinPublicChatUseCase = JoinPublicChatUseCase(userId, mockedRdb)
        return PublicChatServiceImpl(mockedAuth, mockedStorage, mockedRdb, leavePublicChatUseCase, deletePublicChatUseCase,
            joinPublicChatUseCase)
    }

    @Test
    fun leaveChat_isNotAdmin_deletionNotPerformed() {
        runBlocking {
            viewModel.leaveChat(false)
        }
        verify { mockedRdb.child("member_count").child(chatId).setValue(memberCount-1) }
        verify(exactly = 0) { mockedRdb.child("public_chats").child(chatId).removeValue() }
    }
    @Test
    fun leaveChat_isAdmin_deletionPerformed() {
        runBlocking {
            viewModel.leaveChat(true)
            viewModel.uiState.value.alertDialogDataClass.onConfirm()
        }
        verify { mockedRdb.child("public_chats").child(chatId).removeValue() }
        verify { mockedRdb.child("admins").child(chatId).removeValue() }
        verify { mockedRdb.child("members").child(chatId).removeValue() }
        verify { mockedRdb.child("messages").child(chatId).removeValue() }
        verify { mockedRdb.child("member_count").child(chatId).removeValue() }
    }

    @Test
    fun joinChat_success_joinedChat() {
        runBlocking {
            viewModel.joinChat()
        }
        verify { mockedRdb.child("member_count").child(chatId).setValue(memberCount+1) }
    }

    @Test
    fun changeChat_success_changedChat() {
        val newChat = Chat("New", "new")
        runBlocking {
            viewModel.changeChat(newChat.title, newChat.description)
        }
        verify { mockedRdb.child("public_chats").child(chatId).setValue(newChat) }
    }
}