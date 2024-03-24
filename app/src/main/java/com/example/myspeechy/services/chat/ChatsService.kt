package com.example.myspeechy.services.chat

import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.useCases.CheckIfIsAdminUseCase
import com.example.myspeechy.useCases.DeletePublicChatUseCase
import com.example.myspeechy.useCases.FormatDateUseCase
import com.example.myspeechy.useCases.JoinPublicChatUseCase
import com.example.myspeechy.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.useCases.LeavePublicChatUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatsServiceImpl(
    private val database: DatabaseReference,
    auth: FirebaseAuth,
    private val leavePrivateChatUseCase: LeavePrivateChatUseCase,
    private val leavePublicChatUseCase: LeavePublicChatUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val checkIfIsAdminUseCase: CheckIfIsAdminUseCase,
    private val deletePublicChatUseCase: DeletePublicChatUseCase,
    private val formatDateUseCase: FormatDateUseCase
) {
    private var publicChatsStateListener: ChildEventListener? = null
    private var allPublicChatsListener: ChildEventListener? = null
    private var privateChatsStateListener: ChildEventListener? = null
    private var publicChatsListeners: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var privateChatsListeners: MutableMap<String, ValueEventListener> = mutableMapOf()

    val userId = auth.currentUser!!.uid
    private val userRef = database.child("users").child(userId)
    private fun listener(
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataReceived(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
        }
    }
    private fun membershipStateChildListener(
        onAdded: (String) -> Unit,
        onChanged: (String) -> Unit,
        onRemoved: (String) -> Unit,
        onCancelled: (Int) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(snapshot.key!!)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                onChanged(snapshot.key!!)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved(snapshot.key!!)
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }
    }

    private fun allChatsChildListener(onAdded: (Map<String, Chat?>) -> Unit,
                                      onChanged: (Map<String, Chat?>) -> Unit,
                                      onRemoved: (String) -> Unit,
                                      onCancelled: (Int) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(mapOf(snapshot.key!! to snapshot.getValue<Chat>()))
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                onChanged(mapOf(snapshot.key!! to snapshot.getValue<Chat>()))
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved(snapshot.key!!)
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }
    }
    suspend fun searchChatByTitle(title: String): Map<String, Chat>? {
        return database.child("public_chats")
            .orderByChild("title")
            .equalTo(title).get().await().getValue<Map<String, Chat>>()
    }

    fun publicChatsStateListener(
        onAdded: (String) -> Unit,
        onChanged: (String) -> Unit,
        onRemoved: (String) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean) {
        val ref = userRef.child("public_chats")
        if (remove && publicChatsStateListener != null) {
            ref.removeEventListener(publicChatsStateListener!!)
        } else {
            publicChatsStateListener = membershipStateChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(publicChatsStateListener!!)
        }
    }

    fun privateChatsStateListener(
        onAdded: (String) -> Unit,
        onChanged: (String) -> Unit,
        onRemoved: (String) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean) {
        val ref = userRef.child("private_chats")
        if (remove && privateChatsStateListener != null) {
            ref.removeEventListener(privateChatsStateListener!!)
        } else {
            privateChatsStateListener = membershipStateChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(privateChatsStateListener!!)
        }
    }

    fun allPublicChatsListener(
        maxChatIndex: Int,
        onAdded: (Map<String, Chat?>) -> Unit,
        onChanged: (Map<String, Chat?>) -> Unit,
        onRemoved: (String) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean
    ) {
        val ref = database.child("public_chats").limitToFirst(maxChatIndex)
        if (remove && allPublicChatsListener != null) {
            ref.removeEventListener(allPublicChatsListener!!)
        } else {
            allPublicChatsListener = allChatsChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(allPublicChatsListener!!)
        }
    }
    fun handleDynamicAllChatsLoading(
        loadOnResume: Boolean = false,
        maxChatIndex: Int,
        firstVisibleItemIndex: Int?,
        onRemove: () -> Unit,
        onLoad: (Int) -> Unit
    ) {
        if ((firstVisibleItemIndex != null && firstVisibleItemIndex >= maxChatIndex-1 || maxChatIndex == 0)
            && !loadOnResume) {
            if (firstVisibleItemIndex != null) onRemove()
            onLoad(10)
        }
        if (loadOnResume) {
            //load same chats as before by not increasing maxChatBatchIndex
            onLoad(0)
        }
    }
    fun publicChatListener(
        id: String,
        onDataReceived: (DataSnapshot) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean
    ) {
        val ref = database.child("public_chats").child(id)
        val currListener = publicChatsListeners[id]
        if (remove && currListener != null) {
            ref.removeEventListener(currListener)
            publicChatsListeners.remove(id)
        } else {
            publicChatsListeners[id] = listener(onCancelled, onDataReceived)
            ref.addValueEventListener(publicChatsListeners[id]!!)
        }
    }

    fun privateChatListener(
        id: String,
        onDataReceived: (DataSnapshot) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean
    ) {
        val ref = database.child("private_chats")
            .child(userId)
            .child(id)
        val currListener = privateChatsListeners[id]
        if (remove && currListener != null) {
            ref.removeEventListener(currListener)
        } else {
            privateChatsListeners[id] = listener(onCancelled, onDataReceived)
            ref.addValueEventListener(privateChatsListeners[id]!!)
        }
    }

    suspend fun checkIfHasChats(type: String): Boolean {
        return database.child("users")
                .child(userId)
                .child(if (type == "private") "private_chats" else "public_chats")
                .get().await().exists()
    }

    suspend fun createPublicChat(title: String, description: String) {
        val chatId = UUID.randomUUID().toString()
        database.child("admins")
            .child(chatId)
            .setValue(userId).await()
        joinPublicChatUseCase(chatId)
        database.child("public_chats")
            .child(chatId)
            .setValue(Chat(title = title, description = description, type = "public")).await()
    }

    suspend fun deletePublicChat(chatId: String) {
        deletePublicChatUseCase(chatId)
    }
    suspend fun checkIfIsAdmin(chatId: String): Boolean {
        return checkIfIsAdminUseCase(chatId)
    }

    suspend fun leavePrivateChat(chatId: String) {
        leavePrivateChatUseCase(chatId)
    }

    suspend fun leavePublicChat(chatId: String, revokeMembership: Boolean) {
        leavePublicChatUseCase(chatId, revokeMembership)
    }
    fun formatDate(timestamp: Long) = formatDateUseCase(timestamp)
}