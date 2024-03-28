package com.example.myspeechy.domain.chat

import androidx.core.net.toUri
import com.example.myspeechy.domain.auth.AuthService
import com.example.myspeechy.presentation.chat.getOtherUserId
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

enum class PictureStorageError {
    USING_DEFAULT_PROFILE_PICTURE,
    OBJECT_DOES_NOT_EXIST_AT_LOCATION,
}

private val database = Firebase.database.reference
private val storage = Firebase.storage.reference
class UserProfileServiceImpl(private val authService: AuthService) {
    private val usersRef = database.child("users")
    private val picsRef = storage.child("profilePics")
    val userId = authService.userId
    private var userListener: ValueEventListener? = null
    private var picListener: ValueEventListener? = null

    private fun listener(
        onCancelled: (String) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataReceived(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.message)
            }
        }
    }
    fun userListener(
        id: String,
        onCancelled: (String) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean) {
        val ref = usersRef.child(id)
        if (remove && userListener != null) {
            ref.removeEventListener(userListener!!)
        } else {
            userListener = listener(onCancelled, onDataReceived)
            ref.addValueEventListener(userListener!!)
        }
    }


    fun userPictureListener(id: String,
                            file: File,
                            dir: String,
                            onCancelled: (String) -> Unit,
                            onStorageFailure: (String) -> Unit,
                            onPicReceived: () -> Unit,
                            remove: Boolean) {
        val ref = usersRef
            .child(id)
            .child("profilePicUpdated")
        if (remove && picListener != null) {
            ref.removeEventListener(picListener!!)
        } else {
            picListener = listener(onCancelled) { name ->
                val picName = name.getValue(String::class.java)
                if (picName != null) {
                    if (!file.exists()) {
                        DirectoryManager.createPicDir(dir)
                        file.createNewFile()
                    }
                    picsRef.child(id)
                        .child("normalQuality")
                        .child("$id.jpg")
                        .getFile(file)
                        .addOnSuccessListener {
                            it.storage.getFile(file).addOnSuccessListener {
                            onPicReceived()
                        }.addOnFailureListener {onStorageFailure(it.message ?: "")}
                        }
                        .addOnFailureListener { onStorageFailure(it.message ?: "")}
                } else {
                    onStorageFailure(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
                }
            }
            ref.addValueEventListener(picListener!!)
        }
    }
    suspend fun uploadUserPicture(file: File,
                          lowQuality: Boolean) {
        if (userId == null) return
        picsRef.child(userId)
            .child(if (lowQuality) "lowQuality" else "normalQuality")
            .child("$userId.jpg")
            .putFile(file.toUri()).await()
        if (!lowQuality) {
            usersRef.child(userId)
                .child("profilePicUpdated").setValue(UUID.randomUUID().toString()).await()
        }
    }
    suspend fun removeUserPicture(lowQuality: Boolean) {
        if (userId == null) return
        picsRef.child(userId)
            .child(if (lowQuality) "lowQuality" else "normalQuality")
            .child("$userId.jpg")
            .delete().await()
        usersRef.child(userId)
            .child("profilePicUpdated")
            .removeValue().await()
    }
    suspend fun changeUsername(newName: String) {
        if (userId == null) return
        usersRef.child(userId)
            .child("name")
            .setValue(newName).await()
        val privateChats = usersRef.child(userId)
            .child("private_chats")
            .get().await()
        val keys = mutableListOf<String>()
        privateChats.children.forEach { snapshot ->
            keys.add(snapshot.key ?: return@forEach)
        }
        if (keys.isNotEmpty()) return
        keys.forEach {chatId ->
            val otherUserId = chatId.getOtherUserId(userId)
            database.child("private_chats")
                .child(otherUserId)
                .child(chatId)
                .child("title")
                .setValue(newName).await()
        }
    }
    suspend fun changeUserInfo(newInfo: String) {
        if (userId == null) return
        usersRef.child(userId)
            .child("info")
            .setValue(newInfo)
            .await()
    }

    fun logout() {
        authService.logOut()
    }
}