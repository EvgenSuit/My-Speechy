package com.example.myspeechy.services.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.example.myspeechy.utils.chat.getOtherUserId
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

enum class PictureStorageError {
    USING_DEFAULT_PROFILE_PICTURE,
    OBJECT_DOES_NOT_EXIST_AT_LOCATION,
}

private val database = Firebase.database.reference
private val storage = Firebase.storage.reference
class UserProfileServiceImpl {
    private val usersRef = database.child("users")
    private val picsRef = storage.child("profilePics")
    val userId = Firebase.auth.currentUser!!.uid
    private var picListener: ValueEventListener? = null
    private var infoListener: ValueEventListener? = null
    private var nameListener: ValueEventListener? = null
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
    fun usernameListener(
        id: String,
        onCancelled: (Int) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean) {
        val ref = usersRef.child(id)
            .child("name")
        if (remove && nameListener != null) {
            ref.removeEventListener(nameListener!!)
        } else {
            nameListener = listener(onCancelled, onDataReceived)
            ref.addValueEventListener(nameListener!!)
        }
    }
    fun userInfoListener(id: String,
                         onCancelled: (Int) -> Unit,
                         onDataReceived: (DataSnapshot) -> Unit,
                         remove: Boolean) {
        val ref = usersRef.child(id)
            .child("info")
        if (remove && infoListener != null) {
            ref.removeEventListener(infoListener!!)
        } else {
            infoListener = listener(onCancelled, onDataReceived)
            ref.addValueEventListener(infoListener!!)
        }
    }
    fun createPicDir(dir: String) {
        if (!Files.isDirectory(Paths.get(dir))) {
            Files.createDirectories(Paths.get(dir))
        }
    }
    fun compressPicture(imgBytes: ByteArray,
                        quality: Int,
                     onSuccess: (ByteArray) -> Unit,
                     onError: () -> Unit) {
        val baos = ByteArrayOutputStream()
        val bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val compressedBytes = baos.toByteArray()
        if (compressedBytes.size < 2 * 1024 * 1024) { onSuccess(compressedBytes) } else { onError() }
    }
    fun userPictureListener(id: String,
                            file: File,
                            dir: String,
                            onCancelled: (Int) -> Unit,
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
                val picName = name.getValue<String>()
                if (picName != null) {
                    if (!file.exists()) {
                        createPicDir(dir)
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
    fun uploadUserPicture(file: File,
                          lowQuality: Boolean,
                          onError: (String) -> Unit,
                          onSuccess: () -> Unit) {
        picsRef.child(userId)
            .child(if (lowQuality) "lowQuality" else "normalQuality")
            .child("$userId.jpg")
            .putFile(file.toUri())
            .addOnSuccessListener {
                usersRef.child(userId)
                    .child("profilePicUpdated").setValue(UUID.randomUUID().toString()).addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { onError(it.message ?: "") }
            }
            .addOnFailureListener {
                onError(it.message ?: "")}
    }
    fun removeUserPicture(lowQuality: Boolean,
        onError: (String) -> Unit,
                          onSuccess: (String) -> Unit) {
        picsRef.child(userId)
            .child(if (lowQuality) "lowQuality" else "normalQuality")
            .child("$userId.jpg")
            .delete()
            .addOnSuccessListener {
                usersRef.child(userId)
                    .child("profilePicUpdated")
                    .removeValue().addOnSuccessListener {
                        onSuccess(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
                    }.addOnFailureListener { onError(it.message ?: "") }
            }
    }
    fun changeUsername(newName: String, onSuccess: () -> Unit) {
        usersRef.child(userId)
            .child("name")
            .setValue(newName)
        usersRef.child(userId)
            .child("private_chats")
            .get().addOnSuccessListener { chatIds ->
                (chatIds.getValue<Map<String, Boolean>>())?.keys?.forEach{chatId ->
                    val otherUserId = chatId.getOtherUserId(userId)
                    database.child("private_chats")
                        .child(otherUserId)
                        .child(chatId)
                        .child("title")
                        .setValue(newName)
                        .addOnSuccessListener { onSuccess() }
                }
            }
    }
    fun changeUserInfo(newInfo: String, onSuccess: () -> Unit) {
        usersRef.child(userId)
            .child("info")
            .setValue(newInfo)
            .addOnSuccessListener { onSuccess() }
    }
}