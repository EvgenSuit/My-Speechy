package com.example.myspeechy.services.chat

import android.util.Log
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import com.example.myspeechy.data.chat.Message
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

interface RootChatService {
    val userId: String
    val messagesRef: DatabaseReference
    val picsRef: StorageReference
    val usersRef: DatabaseReference
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
        topIndex: Int,
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

    suspend fun sendMessage(chatId: String, senderUsername: String, text: String): Long {
        val timestamp = OffsetDateTime.now(ZoneOffset.UTC).toZonedDateTime().toInstant().toEpochMilli()
        messagesRef.child(chatId)
            .child(UUID.randomUUID().toString())
            .setValue(Message(userId, senderUsername, text, timestamp,false)).await()
        return timestamp
    }
    suspend fun editMessage(chatId: String, message: Map<String, Message>) {
        messagesRef.child(chatId)
            .child(message.keys.first())
            .setValue(message.values.first().copy(edited = true)).await()
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
    //scroll to bottom if the previous message is 100% visible at the time of receiving a new one
    suspend fun scrollToBottom(messages: Map<String, Message>, listState: LazyListState, firstVisibleItem: LazyListItemInfo) {
        val viewportHeight = listState.layoutInfo.viewportEndOffset
        val itemTop = firstVisibleItem.offset
        val itemBottom = itemTop + firstVisibleItem.size
        val isCompletelyVisible = itemTop >= 0 && itemBottom <= viewportHeight
        if (messages.isNotEmpty() &&
            (messages.entries.last().value.sender == userId ||
                    (firstVisibleItem.index == messages.entries.indexOf(messages.entries.first())+1 &&
                            isCompletelyVisible))) {
            listState.animateScrollToItem(0)
        }
    }

    fun handleDynamicMessageLoading(
        loadOnResume: Boolean = false,
        topMessageIndex: Int,
        lastVisibleItemIndex: Int?,
        onRemove: () -> Unit,
        onLoad: (Int) -> Unit) {
        if ((lastVisibleItemIndex != null && lastVisibleItemIndex >= topMessageIndex-1 || topMessageIndex == 0)
            && !loadOnResume) {
            if (lastVisibleItemIndex != null) onRemove()
            onLoad(10)
        }
        if (loadOnResume) {
            //load same messages as before by not increasing topMessageBatchIndex
            onLoad(0)
        }
    }
}
