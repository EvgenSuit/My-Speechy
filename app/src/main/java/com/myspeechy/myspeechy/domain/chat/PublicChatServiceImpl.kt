package com.myspeechy.myspeechy.domain.chat

import com.myspeechy.myspeechy.data.chat.Chat
import com.myspeechy.myspeechy.data.chat.Message
import com.myspeechy.myspeechy.domain.useCases.DeletePublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.JoinPublicChatUseCase
import com.myspeechy.myspeechy.domain.useCases.LeavePublicChatUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class PublicChatServiceImpl(
    auth: FirebaseAuth,
    storage: StorageReference,
    private val database: DatabaseReference,
    private val leavePublicChatUseCase: LeavePublicChatUseCase,
    private val deletePublicChatUseCase: DeletePublicChatUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase
): RootChatService {
    private val chatsRef = database.child("public_chats")
    private val membersRef = database.child("members")

    override val userId = auth.currentUser!!.uid
    override val messagesRef = database.child("messages")
    override val picsRef = storage.child("profilePics")
    override val usersRef = database.child("users")

    private var usernameListeners: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var messagesListener: ChildEventListener? = null
    private var chatListener: ValueEventListener? = null
    private var membershipListener: ChildEventListener? = null
    private var usersProfilePicListeners: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var adminListener: ValueEventListener? = null
    private var messagesStateListener: ValueEventListener? = null
    private var isMemberOfChatListener: ValueEventListener? = null
    private var memberCountListener: ValueEventListener? = null

    private fun chatMembersChildListener(
        onAdded: (Map<String, Boolean>) -> Unit,
        onChanged: (Map<String, Boolean>) -> Unit,
        onRemoved: (Map<String, Boolean>) -> Unit,
        onCancelled: (String) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(mapOf(snapshot.key!! to (snapshot.getValue(Boolean::class.java) ?: false)))
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val map = mutableMapOf<String, Boolean>()
                snapshot.children.forEach { childSnapshot ->
                    val key = childSnapshot.key ?: return
                    val value = childSnapshot.getValue(Boolean::class.java) ?: return
                    map.putAll(mapOf(key to value))
                }
                onChanged(map)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved(mapOf(snapshot.key!! to false))
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.message)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }
    }
    fun checkIfIsMemberOfChat(chatId: String, remove: Boolean,
                              onCancelled: (DatabaseError) -> Unit,
                              isMember: (Boolean) -> Unit) {
        val ref = membersRef.child(chatId).child(userId)
        if (remove && isMemberOfChatListener != null) {
            ref.removeEventListener(isMemberOfChatListener!!)
        } else {
            isMemberOfChatListener = chatEventListener(onCancelled) { isMember(it.exists()) }
            ref.addValueEventListener(isMemberOfChatListener!!)
        }
    }
    fun checkIfChatIsEmpty(chatId: String,
                           remove: Boolean,
                           onCancelled: (DatabaseError) -> Unit,
                           isEmpty: (Boolean) -> Unit) {
        val ref = messagesRef.child(chatId)
        if (remove && messagesStateListener != null) {
            ref.removeEventListener(messagesStateListener!!)
        } else {
            messagesStateListener = chatEventListener(onCancelled) { isEmpty(!it.exists()) }
            ref.addValueEventListener(messagesStateListener!!)
        }
    }
    override fun chatListener(
        id: String,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean
    ) {
        val ref = chatsRef.child(id)
        if (remove && chatListener != null) {
            ref.removeEventListener(chatListener!!)
        } else {
            chatListener = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(chatListener!!)
        }
    }

    override fun messagesListener(
        id: String,
        topIndex: Int,
        onAdded: (Map<String, Message>) -> Unit,
        onChanged: (Map<String, Message>) -> Unit,
        onRemoved: (Map<String, Message>) -> Unit,
        onCancelled: (String) -> Unit,
        remove: Boolean) {
        val ref = messagesRef.child(id)
            .orderByChild("timestamp")
            .limitToLast(topIndex)
        if (remove && messagesListener != null) {
            ref.removeEventListener(messagesListener!!)
        } else {
            messagesListener = messagesChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(messagesListener!!)
        }
    }

    override fun usernameListener(
        id: String?,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean) {
        if (id == null) return
        val ref = database.child("users").child(id)
            .child("name")
        if (remove && usernameListeners[id] != null) {
            ref.removeEventListener(usernameListeners[id]!!)
        } else {
            usernameListeners[id] = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(usernameListeners[id]!!)
        }
    }

    fun memberCountListener(
        id: String?,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean) {
        if (id == null) return
        val ref = database.child("member_count").child(id)
        if (remove && memberCountListener != null) {
            ref.removeEventListener(memberCountListener!!)
        } else {
            memberCountListener = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(memberCountListener!!)
        }
    }

    fun chatMembersListener(
        id: String,
        lastIndex: Int,
        onAdded: (Map<String, Boolean>) -> Unit,
        onChanged: (Map<String, Boolean>) -> Unit,
        onRemoved: (Map<String, Boolean>) -> Unit,
        onCancelled: (String) -> Unit,
        remove: Boolean) {
        val ref = membersRef.child(id).limitToFirst(lastIndex)
        if (remove && membershipListener != null) {
            ref.removeEventListener(membershipListener!!)
        } else {
            membershipListener = chatMembersChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(membershipListener!!)
        }
    }

    fun usersProfilePicListener(id: String,
                                filesDir: String,
                                onCancelled: (DatabaseError) -> Unit,
                                onStorageFailure: (String) -> Unit,
                                onPicReceived: () -> Unit,
                                remove: Boolean) {
        val ref = usersRef
            .child(id)
            .child("profilePicUpdated")
        val currListener = usersProfilePicListeners[id]
        if (remove && currListener != null) {
            ref.removeEventListener(currListener)
        } else {
            usersProfilePicListeners[id] = chatEventListener(onCancelled) { name ->
                picListener(id, name, filesDir, onStorageFailure, onPicReceived)
            }
            ref.addValueEventListener(usersProfilePicListeners[id]!!)
        }
    }
    fun listenForAdmin(chatId: String,
                       onCancelled: (DatabaseError) -> Unit,
                       onDataReceived: (DataSnapshot) -> Unit,
                       remove: Boolean) {
        val ref = database.child("admins").child(chatId)
        if (remove && adminListener != null) {
            ref.removeEventListener(adminListener!!)
        } else {
            adminListener = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(adminListener!!)
        }
    }

    fun handleDynamicMembersLoading(
        loadOnResume: Boolean = false,
        maxMemberIndex: Int,
        lastVisibleItemIndex: Int?,
        onRemove: () -> Unit,
        onLoad: (Int) -> Unit) {
        if ((lastVisibleItemIndex != null && lastVisibleItemIndex >= maxMemberIndex-1 || maxMemberIndex == 0)
            && !loadOnResume) {
            if (lastVisibleItemIndex != null) onRemove()
            onLoad(10)
        }
        if (loadOnResume) {
            //load same members as before by not increasing lastMemberBatchIndex
            onLoad(0)
        }
    }

    suspend fun updateLastMessage(chatId: String, chat: Chat) {
        chatsRef.child(chatId)
            .setValue(chat.copy(lastMessage = if (chat.lastMessage.length > 40)
                chat.lastMessage.substring(0, 40) else chat.lastMessage)).await()
    }
    suspend fun changePublicChat(chatId: String, chat: Chat) {
        database.child("public_chats")
            .child(chatId)
            .setValue(chat).await()
    }

    suspend fun joinChat(chatId: String) {
        joinPublicChatUseCase(chatId)
    }

    suspend fun deletePublicChat(chatId: String) {
        deletePublicChatUseCase(chatId)
        leavePublicChat(chatId, false)
    }
    suspend fun leavePublicChat(chatId: String, revokeMembership: Boolean) {
        leavePublicChatUseCase(chatId, revokeMembership)
    }
}