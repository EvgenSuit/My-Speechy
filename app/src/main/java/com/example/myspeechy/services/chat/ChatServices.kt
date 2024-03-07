package com.example.myspeechy.services.chat

import android.util.Log
import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.useCases.JoinPublicChatUseCase
import com.example.myspeechy.useCases.LeavePrivateChatUseCase
import com.example.myspeechy.useCases.LeavePublicChatUseCase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.notExists

private val database = Firebase.database.reference
private val storage = Firebase.storage.reference
interface ChatService {
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    val messagesRef: DatabaseReference
        get() = database.child("messages")
    val picsRef: StorageReference
        get() = storage.child("profilePics")
    val usersRef: DatabaseReference
        get() = database.child("users")
    fun messagesChildListener(onAdded: (Map<String, Message>) -> Unit,
                      onChanged: (Map<String, Message>) -> Unit,
                      onRemoved: (Map<String, Message>) -> Unit,
                      onCancelled: (Int) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(mapOf(snapshot.key!! to snapshot.getValue<Message>()!!))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                onChanged(mapOf(snapshot.key!! to snapshot.getValue<Message>()!!))
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved(mapOf(snapshot.key!! to Message()))
            }

            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }
        }
    }
    fun chatEventListener(
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
    fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean)

    fun messagesListener(
        id: String,
        onAdded: (Map<String, Message>) -> Unit,
        onChanged: (Map<String, Message>) -> Unit,
        onRemoved: (Map<String, Message>) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean)
    fun usernameListener(id: String,
                        onCancelled: (Int) -> Unit,
                        onDataReceived: (DataSnapshot) -> Unit,
                         remove: Boolean)
    fun picListener(id: String,
                    name: DataSnapshot,
                    filesDir: String,
                    onStorageFailure: (String) -> Unit,
                    onPicReceived: () -> Unit) {
        val picName = name.getValue<String>()
        if (picName != null) {
            val picDir = getPicDir(filesDir, id)
            val file = getPic(filesDir, id)
            if (!file.exists()) {
                createPicDir(picDir)
                file.createNewFile()
            }
            val picRef = File(picDir, "$id.jpg")
            picsRef.child(id)
                .child("lowQuality")
                .child("$id.jpg")
                .getFile(picRef)
                .addOnSuccessListener {
                    //insert picture into the file
                    it.storage.getFile(picRef).addOnSuccessListener { onPicReceived() }
                }
                .addOnFailureListener {onStorageFailure(it.message?:"")}
        } else {
            onStorageFailure(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
        }
    }

    fun sendMessage(chatId: String, senderUsername: String, text: String, replyTo: String): Long {
        val timestamp = System.currentTimeMillis()
        messagesRef.child(chatId)
                    .child(UUID.randomUUID().toString())
                    .setValue(Message(userId, senderUsername, text, timestamp,false, replyTo))
        return timestamp
    }
    fun editMessage(chatId: String, message: Map<String, Message>) {
        messagesRef.child(chatId)
            .child(message.keys.first())
            .setValue(message.values.first().copy(edited = true))
    }
    fun deleteMessage(chatId: String, message: Map<String, Message>) {
        messagesRef.child(chatId)
            .child(message.keys.first())
            .removeValue()
    }

    fun getPicDir(filesDir: String, otherUserId: String): String
            = "${filesDir}/profilePics/$otherUserId/lowQuality"
    fun getPic(filesDir: String, otherUserId: String): File {
        val picDir = getPicDir(filesDir, otherUserId)
        return File(picDir, "$otherUserId.jpg")
    }
    fun createPicDir(picDir: String) {
        if (!Files.isDirectory(Paths.get(picDir))) {
            Files.createDirectories(Paths.get(picDir))
        }
    }

    fun joinChat(chatId: String)
    suspend fun leaveChat(chatId: String)
    fun chatProfilePictureListener(id: String,
                                   filesDir: String,
                                   onCancelled: (Int) -> Unit,
                                   onStorageFailure: (String) -> Unit,
                                   onPicReceived: () -> Unit,
                                   remove: Boolean = false) {}

}

class PublicChatServiceImpl(
    private val leavePublicChatUseCase: LeavePublicChatUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase
): ChatService {
    private val chatsRef: DatabaseReference
        get() = database.child("public_chats")
    private val membersRef: DatabaseReference
        get() = database.child("members")
    private var usernameListeners: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var messagesListener: ChildEventListener? = null
    private var chatListener: ValueEventListener? = null
    private var membershipListener: ChildEventListener? = null
    private var usersProfilePicListeners: MutableMap<String, ValueEventListener> = mutableMapOf()

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
        onAdded: (Map<String, Message>) -> Unit,
        onChanged: (Map<String, Message>) -> Unit,
        onRemoved: (Map<String, Message>) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean) {
        val ref = messagesRef.child(id)
            .orderByChild("timestamp")
            //.limitToLast(5)
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

    fun updateLastMessage(chatId: String, chat: Chat, onSuccess: () -> Unit = {}) {
        val publicChat = chat.copy(type = "public")
        chatsRef.child(chatId)
            .setValue(publicChat)
            .addOnSuccessListener { onSuccess() }
    }

    override fun joinChat(chatId: String) {
        joinPublicChatUseCase(chatId)
    }

    override suspend fun leaveChat(chatId: String) {
        leavePublicChatUseCase(chatId)
    }

}

class PrivateChatServiceImpl(
    private val leavePrivateChatUseCase: LeavePrivateChatUseCase
): ChatService {
    private val chatsRef: DatabaseReference
        get() = database.child("private_chats")
    private var chatListener: ValueEventListener? = null
    private var usernamesListener: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var chatPicListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null
    private var isMemberOfChatListener: ValueEventListener? = null
    override fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean) {
        val ref = chatsRef.child(userId).child(id)
        if (remove && chatListener != null) {
            ref.removeEventListener(chatListener!!)
        } else {
            chatListener = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(chatListener!!)
        }
    }
    override fun messagesListener(
        id: String,
        onAdded: (Map<String, Message>) -> Unit,
        onChanged: (Map<String, Message>) -> Unit,
        onRemoved: (Map<String, Message>) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean
    ) {
        val ref = messagesRef.child(id)
            .orderByChild("timestamp")
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
        if (remove && usernamesListener[id] != null) {
            ref.removeEventListener(usernamesListener[id]!!)
        } else {
            usernamesListener[id] = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(usernamesListener[id]!!)
        }
    }

    fun updateLastMessage(chatId: String, currentUsername: String? = null, otherUsername: String? = null, chat: Chat,
                          onSuccess: () -> Unit = {}) {
        val userIds = chatId.split("_")
        val privateChat = chat.copy(type = "private", lastMessage = if (chat.lastMessage.length > 40) chat.lastMessage.substring(0, 40)
        else chat.lastMessage)
        //Update chat title for each of the users
        if (currentUsername != null && (userIds[0] == userId || userIds[1] == userId)) {
            chatsRef.child(userIds[1])
                .child(chatId)
                .setValue(privateChat.copy(title = currentUsername))
        }
        if (otherUsername != null && (userIds[0] != userId || userIds[1] != userId)) {
            chatsRef.child(userIds[0])
                .child(chatId)
                .setValue(privateChat.copy(title = otherUsername))
        }
    }

    override fun joinChat(chatId: String) {
        val userIds = chatId.split("_")
        userIds.forEach { id ->
            chatsRef.child(id)
                .child(chatId)
                .setValue(Chat())
            usersRef.child(id)
                .child("private_chats")
                .child(chatId)
                .setValue(true)
        }
    }

    override fun createPicDir(picDir: String) {
        if (!Files.isDirectory(Paths.get(picDir))) {
            Files.createDirectories(Paths.get(picDir))
        }
    }

    override fun chatProfilePictureListener(id: String,
                                            filesDir: String,
                                            onCancelled: (Int) -> Unit,
                                            onStorageFailure: (String) -> Unit,
                                            onPicReceived: () -> Unit,
                                            remove: Boolean) {
        val ref = usersRef
                .child(id)
                .child("profilePicUpdated")
        if (remove && chatPicListener != null) {
            ref.removeEventListener(chatPicListener!!)
        } else {
            chatPicListener = chatEventListener(onCancelled) { name ->
                val picName = name.getValue<String>()
                if (picName != null) {
                    val picDir = getPicDir(filesDir, id)
                    if (Paths.get(picDir).notExists() || !getPic(filesDir, id).exists()) {
                        createPicDir(picDir)
                        getPic(filesDir, id).createNewFile()
                    }
                    val picRef = File(picDir, "$id.jpg")
                    picsRef.child(id)
                        .child("lowQuality")
                        .child("$id.jpg")
                        .getFile(picRef)
                        .addOnSuccessListener {
                            //insert picture into the file
                            it.storage.getFile(picRef).addOnSuccessListener { onPicReceived() }
                        }
                        .addOnFailureListener {onStorageFailure(it.message?:"")}
                } else {
                    onStorageFailure(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
                }
            }
            ref.addValueEventListener(chatPicListener!!)
        }
    }

    override suspend fun leaveChat(chatId: String) {
        leavePrivateChatUseCase(chatId)
    }
    fun listenIfIsMemberOfChat(chatId: String, onReceived: (Boolean) -> Unit, remove: Boolean) {
        val ref = usersRef.child(userId)
            .child("private_chats")
            .child(chatId)
        if (remove && isMemberOfChatListener != null) {
            ref.removeEventListener(isMemberOfChatListener!!)
        } else {
            isMemberOfChatListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onReceived(snapshot.exists())
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(isMemberOfChatListener!!)
        }
    }
}