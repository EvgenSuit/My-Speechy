package com.example.myspeechy.services.chat

import androidx.core.net.toUri
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

enum class PictureStorageError {
    USING_DEFAULT_PROFILE_PICTURE,
    OBJECT_DOES_NOT_EXIST_AT_LOCATION
}

private val database = Firebase.database.reference
private val storage = Firebase.storage.reference
interface UserProfileService {
    private val usersRef: DatabaseReference
        get() = database.child("users")
    private val picsRef: StorageReference
        get() = storage.child("profilePics")
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
        onDataReceived: (DataSnapshot) -> Unit) {
        usersRef.child(id)
            .child("name")
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun userInfoListener(id: String,
                         onCancelled: (Int) -> Unit,
                         onDataReceived: (DataSnapshot) -> Unit) {
        usersRef.child(id)
            .child("info")
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun userPictureListener(id: String,
                            file: File,
                            onCancelled: (Int) -> Unit,
                            onStorageFailure: (String) -> Unit,
                            onPicReceived: () -> Unit) {
        usersRef
            .child(id)
            .child("profilePicUpdated")
            .addValueEventListener(listener(onCancelled) { name ->
                val picName = name.getValue<String>()
                if (!picName.isNullOrEmpty()) {
                    picsRef.child(id)
                        .child(picName).getFile(file)
                        .addOnSuccessListener { it.storage.getFile(file)
                        onPicReceived()
                        }
                        .addOnFailureListener { onStorageFailure(it.message ?: "")}
                } else {
                    onStorageFailure(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
                }
            })
    }
    fun uploadUserPicture(id: String,
                       file: File,
                       onSuccess: () -> Unit) {
        picsRef.child(id)
            .child("$id.jpg")
            .putFile(file.toUri())
            .addOnSuccessListener {
                usersRef.child(id)
                    .child("profilePicUpdated").setValue("$id.jpg").addOnSuccessListener {
                        onSuccess()
                    }
            }
    }
    fun removeUserPicture(id: String,
                          onSuccess: (String) -> Unit) {
        picsRef.child(id)
            .child("$id.jpg")
            .delete()
            .addOnSuccessListener {
                usersRef.child(id)
                    .child("profilePicUpdated")
                    .removeValue().addOnSuccessListener {
                        onSuccess(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
                    }
            }
    }
    fun changeUsername(id: String,
                              newName: String) {
        usersRef.child(id)
            .child("name")
            .setValue(newName)
    }
    fun changeUserInfo(id: String,
                       newInfo: String) {
        usersRef.child(id)
            .child("info")
            .setValue(newInfo)
    }
}
class UserProfileServiceImpl: UserProfileService