package com.example.myspeechy.services.chat

import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.useCases.FormatDateUseCase
import com.example.myspeechy.useCases.JoinPublicChatUseCase
import com.example.myspeechy.useCases.LeavePublicChatUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class PublicChatServiceImpl(
    auth: FirebaseAuth,
    storage: StorageReference,
    private val database: DatabaseReference,
    private val leavePublicChatUseCase: LeavePublicChatUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val formatDateUseCase: FormatDateUseCase
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

    private fun chatMembersChildListener(
        onAdded: (Map<String, Boolean>) -> Unit,
        onChanged: (Map<String, Boolean>) -> Unit,
        onRemoved: (Map<String, Boolean>) -> Unit,
        onCancelled: (Int) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(mapOf(snapshot.key!! to (snapshot.getValue<Boolean>() ?: false)))
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue<Map<String, Boolean>>()?.let { onChanged(it) }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved(mapOf(snapshot.key!! to false))
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }
    }
    fun checkIfIsMemberOfChat(chatId: String, remove: Boolean, isMember: (Boolean) -> Unit) {
        val ref = membersRef.child(chatId).child(userId)
        if (remove && isMemberOfChatListener != null) {
            ref.removeEventListener(isMemberOfChatListener!!)
        } else {
            isMemberOfChatListener = chatEventListener({}, {isMember(it.exists())})
            ref.addValueEventListener(isMemberOfChatListener!!)
        }
    }
    fun checkIfChatIsEmpty(chatId: String, remove: Boolean, isEmpty: (Boolean) -> Unit) {
        val ref = messagesRef.child(chatId)
        if (remove && messagesStateListener != null) {
            ref.removeEventListener(messagesStateListener!!)
        } else {
            messagesStateListener = chatEventListener({}, {isEmpty(!it.exists())})
            ref.addValueEventListener(messagesStateListener!!)
        }
    }
    override fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
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
        onCancelled: (Int) -> Unit,
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
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean
    ) {
        val ref = database.child("users").child(id)
            .child("name")
        if (remove && usernameListeners[id] != null) {
            ref.removeEventListener(usernameListeners[id]!!)
        } else {
            usernameListeners[id] = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(usernameListeners[id]!!)
        }
    }

    fun chatMembersListener(
        id: String,
        onAdded: (Map<String, Boolean>) -> Unit,
        onChanged: (Map<String, Boolean>) -> Unit,
        onRemoved: (Map<String, Boolean>) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean) {
        val ref = membersRef.child(id)
        if (remove && membershipListener != null) {
            ref.removeEventListener(membershipListener!!)
        } else {
            membershipListener = chatMembersChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(membershipListener!!)
        }
    }

    fun usersProfilePicListener(id: String,
                                filesDir: String,
                                onCancelled: (Int) -> Unit,
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
                       onCancelled: (Int) -> Unit,
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

    /*fun handleDynamicMembersLoading(
        loadOnResume: Boolean = false,
        maxMemberIndex: Int,
        lastVisibleItemIndex: Int?,
        onRemove: () -> Unit,
        onLoad: (Int) -> Unit
    ) {
        if ((lastVisibleItemIndex != null && lastVisibleItemIndex >= maxMemberIndex-1 || maxMemberIndex == 0)
            && !loadOnResume) {
            if (lastVisibleItemIndex != null) onRemove()
            onLoad(1)
        }
        if (loadOnResume) {
            //load same members as before by not increasing lastMemberBatchIndex
            onLoad(0)
        }
    }*/

    suspend fun updateLastMessage(chatId: String, chat: Chat) {
        chatsRef.child(chatId)
            .setValue(chat.copy(lastMessage = if (chat.lastMessage.length > 40)
                chat.lastMessage.substring(0, 40) else chat.lastMessage)).await()
    }
    fun changePublicChat(chatId: String, chat: Chat) {
        database.child("public_chats")
            .child(chatId)
            .setValue(chat)
    }

    suspend fun joinChat(chatId: String) {
        joinPublicChatUseCase(chatId)
    }

    suspend fun leaveChat(chatId: String) {
        leavePublicChatUseCase(chatId)
    }

    fun formatDate(timestamp: Long) = formatDateUseCase(timestamp)
}