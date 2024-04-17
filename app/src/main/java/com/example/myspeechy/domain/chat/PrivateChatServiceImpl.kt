package com.example.myspeechy.domain.chat

import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.domain.error.PictureStorageError
import com.example.myspeechy.domain.useCases.LeavePrivateChatUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.notExists

class PrivateChatServiceImpl(
    auth: FirebaseAuth,
    storage: StorageReference,
    private val database: DatabaseReference,
    private val leavePrivateChatUseCase: LeavePrivateChatUseCase
): RootChatService {
    override val userId = auth.currentUser?.uid
    override val messagesRef = database.child("messages")
    override val picsRef = storage.child("profilePics")
    override val usersRef = database.child("users")

    private val chatsRef = database.child("private_chats")
    private var chatListener: ValueEventListener? = null
    private var usernamesListener: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var chatPicListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null
    private var isMemberOfChatListener: ValueEventListener? = null
    private var messagesStateListener: ValueEventListener? = null
    override fun chatListener(
        id: String,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean) {
        if (userId == null) return
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
        topIndex: Int,
        onAdded: (Map<String, Message>) -> Unit,
        onChanged: (Map<String, Message>) -> Unit,
        onRemoved: (Map<String, Message>) -> Unit,
        onCancelled: (String) -> Unit,
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
        id: String?,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean
    ) {
        if (id == null) return
        val ref = database.child("users").child(id)
            .child("name")
        if (remove && usernamesListener[id] != null) {
            ref.removeEventListener(usernamesListener[id]!!)
        } else {
            usernamesListener[id] = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(usernamesListener[id]!!)
        }
    }

    suspend fun updateLastMessage(chatId: String, currentUsername: String? = null, otherUsername: String? = null, chat: Chat) {
        val userIds = chatId.split("_")
        val privateChat = chat.copy(lastMessage = if (chat.lastMessage.length > 40) chat.lastMessage.substring(0, 40) else chat.lastMessage)
        //Update chat title for each of the users
        if (currentUsername != null && otherUsername != null && userId != null) {
            chatsRef.child(if (userIds[0] == userId) userId else userIds[1])
                .child(chatId)
                .setValue(privateChat.copy(title = if (userIds[0] == userId) otherUsername else currentUsername)).await()
            chatsRef.child(if (userIds[0] == userId) userIds[1] else userId)
                .child(chatId)
                .setValue(privateChat.copy(title = if (userIds[0] == userId) currentUsername else otherUsername)).await()
        }
    }

    suspend fun joinChat(chatId: String) {
        val userIds = chatId.split("_")
        userIds.forEach { id ->
            chatsRef.child(id)
                .child(chatId)
                .setValue(Chat(type = "private")).await()
            usersRef.child(id)
                .child("private_chats")
                .child(chatId)
                .setValue(true).await()
        }
    }

    override fun createPicDir(picDir: String) {
        if (!Files.isDirectory(Paths.get(picDir))) {
            Files.createDirectories(Paths.get(picDir))
        }
    }

    fun chatProfilePictureListener(id: String,
                                   filesDir: String,
                                   onCancelled: (DatabaseError) -> Unit,
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
                val picName = name.getValue(String::class.java)
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

    suspend fun leaveChat(chatId: String) {
        leavePrivateChatUseCase(chatId)
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
    fun listenIfIsMemberOfChat(userId: String, chatId: String,
                               onCancelled: (String) -> Unit,
                               onReceived: (Boolean) -> Unit, remove: Boolean) {
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
                override fun onCancelled(error: DatabaseError) {
                    onCancelled(error.message)
                }
            }
            ref.addValueEventListener(isMemberOfChatListener!!)
        }
    }
}