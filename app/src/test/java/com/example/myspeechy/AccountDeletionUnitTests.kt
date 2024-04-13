package com.example.myspeechy

import com.example.myspeechy.domain.auth.AccountDeletionService
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.domain.useCases.CheckIfIsAdminUseCase
import com.example.myspeechy.domain.useCases.DecrementMemberCountUseCase
import com.example.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.example.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.domain.useCases.LeavePublicChatUseCase
import com.example.myspeechy.presentation.auth.AccountDeletionViewModel
import com.example.myspeechy.presentation.chat.getOtherUserId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountDeletionUnitTests {
    private val imgQualities = listOf("normalQuality", "lowQuality")
    private val firestoreCollections = listOf("lessons", "meditation")
    private val lessons = (1..3).map { mapOf("$it" to mapOf("id" to "$it")) }
    private val privateChats = listOf("dfdf_$userId", "${userId}_38vhje")
    private val publicChats = mapOf("dfjidjf" to mapOf("admin" to "dffg", "member_count" to 12),
        "ejiggrj3" to mapOf("admin" to userId, "member_count" to 1))
    private var mockedAuth = mockk<FirebaseAuth>()
    private lateinit var mockedFirestore: FirebaseFirestore
    private lateinit var mockedStorage: StorageReference
    private lateinit var mockedRdb: DatabaseReference
    private lateinit var viewModel: AccountDeletionViewModel

    @Before
    fun mockDependencies() {
        mockAuth()
        mockStorage()
        mockRdb()
        mockFirestore()
        mockViewModel()
    }
    fun mockViewModel() {
        val decrementMemberCountUseCase = DecrementMemberCountUseCase(mockedRdb)
        val authService = AuthService(mockedAuth, mockedRdb, mockedFirestore, mockedStorage,
            LeavePublicChatUseCase(userId, mockedRdb, decrementMemberCountUseCase),
            LeavePrivateChatUseCase(userId, mockedRdb),
            CheckIfIsAdminUseCase(userId, mockedRdb),
            DeletePublicChatUseCase(mockedRdb, decrementMemberCountUseCase)
        )
        viewModel = AccountDeletionViewModel(AccountDeletionService(authService, mockedAuth))
    }
    fun mockFirestore() {
        val mockedLessons = mockk<QuerySnapshot>{
            every { documents } returns lessons.map {
                mockk<DocumentSnapshot> {
                    every { id } returns it.keys.first()
                }
            }
        }
        mockedFirestore = mockk<FirebaseFirestore> {
            for (coll in firestoreCollections) {
                every { collection("users").document(userId).collection(coll).get() } returns mockTask(mockedLessons)
                for (l in lessons) {
                    every { collection("users").document(userId).collection(coll).document(l.keys.first()).delete() } returns mockTask()
                }
            }
        }
    }
    fun mockAuth() {
        mockedAuth = mockk<FirebaseAuth> {
            every { currentUser } returns mockk<FirebaseUser>()
            every { currentUser?.delete() } answers {
                every { currentUser } returns null
                mockTask()
            }
            every { currentUser?.uid } returns userId
            every { currentUser?.displayName } returns username
        }
    }

    fun mockStorage() {
        mockedStorage = mockk<StorageReference> {
            for (quality in imgQualities)
                every { child("profilePics").child(userId).child(quality).child("$userId.jpg").delete() } returns mockTask()
        }
    }

    fun mockRdb() {
        val mockedProfilePicUpdatedSnapshot = mockk<DataSnapshot> {
            every { exists() } returns true
        }
        val mockedPrivateChats = mockk<DataSnapshot>{
            every { children } returns privateChats.map { mockk<DataSnapshot>{
                every { key } returns it
            } }
        }
        val mockedPublicChats = mockk<DataSnapshot>{
            every { children } returns publicChats.keys.map { mockk<DataSnapshot>{
                every { key } returns it
            } }
        }
        mockedRdb = mockk<DatabaseReference>{
            every { child("users").child(userId).removeValue() } returns mockTask()
            every { child("users").child(userId).child("profilePicUpdated").get() } returns mockTask(mockedProfilePicUpdatedSnapshot)
            every { child("users").child(userId).child("private_chats").get() } returns mockTask(mockedPrivateChats)
            every { child("users").child(userId).child("public_chats").get() } returns mockTask(mockedPublicChats)
            for (chatId in privateChats) {
                val mockedOtherUserHasChat = mockk<DataSnapshot>{
                    every { exists() } returns true
                }
                every { child("users").child(chatId.getOtherUserId(userId)).child("private_chats").child(chatId).get() } returns mockTask(mockedOtherUserHasChat)
                every { child("users").child(userId).child("private_chats").child(chatId).removeValue() } returns mockTask()
                every { child("private_chats").child(userId).child(chatId).removeValue() } returns mockTask()
            }
            for ((chatId, entry) in publicChats) {
                val admin = entry["admin"] as String
                val mockedAdmin = mockk<DataSnapshot>{
                    every { getValue(String::class.java) } returns admin
                }
                val memberCount = entry["member_count"] as Int
                val mockedMemberCount = mockk<DataSnapshot> {
                    every { getValue(Int::class.java) } returns memberCount
                }
                every { child("admins").child(chatId).get() } returns mockTask(mockedAdmin)
                every { child("public_chats").child(chatId).removeValue() } returns mockTask()
                every { child("messages").child(chatId).removeValue() } returns mockTask()
                every { child("member_count").child(chatId).get() } returns mockTask(mockedMemberCount)
                if (memberCount-1 <= 0 || admin == userId) every { child("member_count").child(chatId).removeValue() } returns mockTask()
                else every { child("member_count").child(chatId).setValue(memberCount-1) } returns mockTask()
                every { child("members").child(chatId).removeValue() } returns mockTask()
                every { child("admins").child(chatId).removeValue() } returns mockTask()
                if (admin != userId) every { child("members").child(chatId).child(userId).removeValue() } returns mockTask()
                every { child("users").child(userId).child("public_chats").child(chatId).removeValue() } returns mockTask()
            }
            every { child("messages").child(any()).removeValue() } returns mockTask()
        }
    }
    @Test
    fun deleteAccount_userIsNull_deletionNotPerformed() {
        every { mockedAuth.currentUser?.uid } returns null
        runBlocking {
            viewModel.deleteUser()
        }
        for (quality in listOf("normalQuality", "lowQuality")) {
            verify(exactly = 0) { mockedStorage.child("profilePics").child(userId).child(quality).child("$userId.jpg").delete() }
        }
    }

    @Test
    fun deleteAccount_userIsNotNull_deletionPerformed() {
        runBlocking {
            viewModel.deleteUser()
        }
        assertTrue(viewModel.uiState.value.result.error.isEmpty())
        for (quality in listOf("normalQuality", "lowQuality")) {
            verify { mockedStorage.child("profilePics").child(userId).child(quality).child("$userId.jpg").delete() }
        }
        for (chatId in privateChats) {
            verify { mockedRdb.child("private_chats").child(userId).child(chatId).removeValue() }
        }
        for ((chatId, entry) in publicChats) {
            val admin = entry["admin"] as String
            val ref = mockedRdb.child("public_chats").child(chatId)
            verify(exactly = if (admin == userId) 1 else 0) { ref.removeValue()}
        }
        for (coll in firestoreCollections) {
            verify { mockedFirestore.collection("users").document(userId).collection(coll).get() }
            for (l in lessons) {
                verify { mockedFirestore.collection("users").document(userId).collection(coll).document(l.keys.first()).delete() }
            }
        }
        verify { mockedRdb.child("users").child(userId).removeValue() }
        verify { mockedAuth.currentUser?.delete() }
    }
}