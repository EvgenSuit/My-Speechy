package com.example.myspeechy.services.chat

import com.example.myspeechy.data.chat.Chat
import com.example.myspeechy.data.chat.Message
import com.google.firebase.auth.ktx.auth
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
    val membersRef: DatabaseReference
        get() = database.child("members")
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
        onDataReceived: (DataSnapshot) -> Unit) {}

    fun messagesListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (List<DataSnapshot>) -> Unit) {
        messagesRef.child(id)
            .orderByChild("timestamp")
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun chatMembersListener(id: String,
                            onCancelled: (Int) -> Unit,
                            onDataReceived: (List<DataSnapshot>) -> Unit) {}
    fun usernameListener(id: String,
                        onCancelled: (Int) -> Unit,
                        onDataReceived: (DataSnapshot) -> Unit) {
        database.child("users").child(id)
            .child("name").addValueEventListener(chatEventListener(onCancelled, onDataReceived))
    }
    fun sendMessage(chatId: String, chatTitle: String, text: String): Long {
        val timestamp = System.currentTimeMillis()
        messagesRef.child(chatId)
                    .child(UUID.randomUUID().toString())
                    .setValue(Message(userId, "", text, timestamp))
        return timestamp
    }

    fun updateLastMessage(chatId: String,chat: Chat) {}
    fun joinChat(chatId: String) {}
    fun createPicDir(picDir: String) {}
    fun chatProfilePictureListener(id: String,
                                   filesDir: String,
                                   onCancelled: (Int) -> Unit,
                                   onStorageFailure: (String) -> Unit,
                                   onPicReceived: () -> Unit) {}

}

class PublicChatServiceImpl: ChatService {
    private val chatsRef: DatabaseReference
        get() = database.child("public_chats")
    override fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit
    ) {
        chatsRef.child(id)
            .addValueEventListener(chatEventListener(onCancelled, onDataReceived))
    }

    override fun chatMembersListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (List<DataSnapshot>) -> Unit
    ) {
        membersRef
            .child(id)
            .addValueEventListener(listener(onCancelled, onDataReceived))
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
    override fun chatListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit
    ) {
        chatsRef.child(userId).child(id)
            .addValueEventListener(chatEventListener(onCancelled, onDataReceived))
    }

    //Todo call this method when updating username
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
                                            onPicReceived: () -> Unit) {
        usersRef
                .child(id)
                .child("profilePicUpdated")
                .addValueEventListener(chatEventListener(onCancelled) { name ->
                    val picName = name.getValue<String>()
                    if (!picName.isNullOrEmpty()) {
                        val picDir = getChatPicDir(filesDir, id)
                        createPicDir(picDir)
                        val picRef = File(picDir, "$id.jpg")
                        if (Paths.get(picDir).notExists()) {
                            createPicDir(picDir)
                        }
                        picsRef.child(id)
                            .child(picName).getFile(picRef)
                            .addOnSuccessListener {
                                it.storage.getFile(picRef)
                                onPicReceived()
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
                })
    }
}