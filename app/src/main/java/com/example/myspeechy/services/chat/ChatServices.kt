package com.example.myspeechy.services.chat

import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
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
import java.util.Locale
import java.util.UUID
import kotlin.io.path.notExists

private val database = Firebase.database.reference
private val storage = Firebase.storage.reference
interface ChatService {
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    val messagesRef: DatabaseReference
        get() = database.child("messages")
    fun listener(
        onCancelled: (Int) -> Unit,
        onDataReceived: (List<DataSnapshot>) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataReceived(snapshot.children.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
        }
    }
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
        remove: Boolean) {}
    fun usernameListener(id: String,
                        onCancelled: (Int) -> Unit,
                        onDataReceived: (DataSnapshot) -> Unit,
                         remove: Boolean) {

    }

    fun sendMessage(chatId: String, senderUsername: String, text: String): Long {
        val timestamp = System.currentTimeMillis()
        messagesRef.child(chatId)
                    .child(UUID.randomUUID().toString())
                    .setValue(Message(userId, senderUsername, text, timestamp))
        return timestamp
    }

    fun updateLastMessage(chatId: String,chat: Chat)
    fun joinChat(chatId: String)
    fun createPicDir(picDir: String) {}
    fun chatProfilePictureListener(id: String,
                                   filesDir: String,
                                   onCancelled: (Int) -> Unit,
                                   onStorageFailure: (String) -> Unit,
                                   onPicReceived: () -> Unit,
                                   remove: Boolean = false) {}

}

class PublicChatServiceImpl: ChatService {
    private val chatsRef: DatabaseReference
        get() = database.child("public_chats")
    private val membersRef: DatabaseReference
        get() = database.child("members")
    private var usernameListeners: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var messagesListener: ChildEventListener? = null
    private var chatListener: ValueEventListener? = null
    private var membershipListener: ChildEventListener? = null

    private fun chatMembersChildListener(
        onAdded: (Map<String, String>) -> Unit,
        onChanged: (Map<String, String>) -> Unit,
        onRemoved: (Map<String, String>) -> Unit,
        onCancelled: (Int) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(mapOf(snapshot.key!! to (snapshot.value as String)))
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                onChanged(mapOf(snapshot.key!! to (snapshot.value as String)))
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved(mapOf(snapshot.key!! to ""))
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
        onAdded: (Map<String, String>) -> Unit,
        onChanged: (Map<String, String>) -> Unit,
        onRemoved: (Map<String, String>) -> Unit,
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

    override fun updateLastMessage(chatId: String, chat: Chat) {
        val publicChat = chat.copy(type = "public")
        chatsRef.child(chatId)
            .setValue(publicChat)
    }

    override fun joinChat(chatId: String) {
        membersRef.child(chatId).setValue(mapOf(userId to true))
    }

}

class PrivateChatServiceImpl: ChatService {
    private val chatsRef: DatabaseReference
        get() = database.child("private_chats")
    private val usersRef: DatabaseReference
        get() = database.child("users")
    private val picsRef: StorageReference
        get() = storage.child("profilePics")
    private var chatListener: ValueEventListener? = null
    private var usernameListener: ValueEventListener? = null
    private var chatPicListener: ValueEventListener? = null
    private var messagesListener: ChildEventListener? = null
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
        if (remove && usernameListener != null) {
            ref.removeEventListener(usernameListener!!)
        } else {
            usernameListener = chatEventListener(onCancelled, onDataReceived)
            ref.addValueEventListener(usernameListener!!)
        }
    }

    override fun updateLastMessage(chatId: String, chat: Chat) {
        val userIds = chatId.split("_")
        val privateChat = chat.copy(type = "private")
        //Update chat title for each of the users
        usersRef.child(userIds[1]).child("name").get().addOnSuccessListener { username ->
             chatsRef.child(userIds[0])
                 .child(chatId)
                 .setValue(privateChat.copy(title = username.getValue<String>() ?: ""))
         }
        usersRef.child(userIds[0]).child("name").get().addOnSuccessListener { username ->
            chatsRef.child(userIds[1])
                .child(chatId)
                .setValue(privateChat.copy(title = username.getValue<String>() ?: ""))
        }
    }

    override fun joinChat(chatId: String) {
        val userIds = chatId.split("_")
        chatsRef.child(userIds[0])
            .child(chatId)
            .setValue(Chat())
        chatsRef.child(userIds[1])
            .child(chatId)
            .setValue(Chat())
        usersRef.child(userIds[0])
            .child("private_chats")
            .setValue(mapOf(chatId to true))
        usersRef.child(userIds[1])
            .child("private_chats")
            .setValue(mapOf(chatId to true))
    }

    override fun createPicDir(picDir: String) {
        if (!Files.isDirectory(Paths.get(picDir))) {
            Files.createDirectories(Paths.get(picDir))
        }
    }
    fun getChatPicDir(filesDir: String, otherUserId: String): String
    = "${filesDir}/profilePics/$otherUserId"
    fun getChatPic(filesDir: String, otherUserId: String): File {
        val picDir = getChatPicDir(filesDir, otherUserId)
        return File(picDir, "$otherUserId.jpg")
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
                if (!picName.isNullOrEmpty()) {
                    val picDir = getChatPicDir(filesDir, id)
                    if (Paths.get(picDir).notExists()) {
                        createPicDir(picDir)
                    }
                    val picRef = File(picDir, "$id.jpg")
                    picsRef.child(id)
                        .child(picName).getFile(picRef)
                        .addOnSuccessListener {
                            //insert picture into the file
                            it.storage.getFile(picRef).addOnSuccessListener { onPicReceived() }
                        }
                        .addOnFailureListener {
                            val message = it.message?.split(" ")?.joinToString("_") { it.uppercase(
                                Locale.ROOT) }?.dropLast(1) ?: ""
                            onStorageFailure(message)
                            if (message == PictureStorageError.OBJECT_DOES_NOT_EXIST_AT_LOCATION.name) {
                                if (picRef.exists()) {
                                    picRef.delete()
                                }
                                Files.deleteIfExists(Paths.get(picDir))
                            }
                        }
                } else {
                    onStorageFailure(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
                }
            }
            ref.addValueEventListener(chatPicListener!!)
        }
    }
}