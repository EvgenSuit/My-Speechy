package com.example.myspeechy.domain.chat

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import com.example.myspeechy.data.chat.Message
import com.example.myspeechy.domain.error.PictureStorageError
import com.example.myspeechy.domain.useCases.FormatDateUseCase
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

interface RootChatService {
    val userId: String?
    val messagesRef: DatabaseReference
    val picsRef: StorageReference?
    val usersRef: DatabaseReference?
    val formatDateUseCase: FormatDateUseCase
        get() = FormatDateUseCase()
    fun messagesChildListener(onAdded: (Map<String, Message>) -> Unit,
                              onChanged: (Map<String, Message>) -> Unit,
                              onRemoved: (Map<String, Message>) -> Unit,
                              onCancelled: (String) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(mapOf(snapshot.key!! to snapshot.getValue(Message::class.java)!!))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                onChanged(mapOf(snapshot.key!! to snapshot.getValue(Message::class.java)!!))
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved(mapOf(snapshot.key!! to Message()))
            }

            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.message)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }
        }
    }
    fun chatEventListener(
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataReceived(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error)
            }
        }
    }
    fun chatListener(
        id: String,
        onCancelled: (DatabaseError) -> Unit,
        onDataReceived: (DataSnapshot) -> Unit,
        remove: Boolean)

    fun messagesListener(
        id: String,
        topIndex: Int,
        onAdded: (Map<String, Message>) -> Unit,
        onChanged: (Map<String, Message>) -> Unit,
        onRemoved: (Map<String, Message>) -> Unit,
        onCancelled: (String) -> Unit,
        remove: Boolean)
    fun usernameListener(id: String?,
                         onCancelled: (DatabaseError) -> Unit,
                         onDataReceived: (DataSnapshot) -> Unit,
                         remove: Boolean)
    fun picListener(id: String,
                    name: DataSnapshot,
                    filesDir: String,
                    onStorageFailure: (String) -> Unit,
                    onPicReceived: () -> Unit) {
        val picName = name.getValue(String::class.java)
        if (picName != null) {
            val picDir = getPicDir(filesDir, id)
            val file = getPic(filesDir, id)
            if (!file.exists()) {
                createPicDir(picDir)
                file.createNewFile()
            }
            val picRef = File(picDir, "$id.jpg")
            picsRef?.child(id)
                ?.child("lowQuality")
                ?.child("$id.jpg")
                ?.getFile(picRef)
                ?.addOnSuccessListener {
                    onPicReceived()
                }
                ?.addOnFailureListener {onStorageFailure(it.message?:"")}
        } else {
            onStorageFailure(PictureStorageError.USING_DEFAULT_PROFILE_PICTURE.name)
        }
    }

    suspend fun sendMessage(chatId: String, senderUsername: String, text: String): Long {
        val timestamp = OffsetDateTime.now(ZoneOffset.UTC).toZonedDateTime().toInstant().toEpochMilli()
        val id = userId
        if (id != null)
            messagesRef.child(chatId)
                .child(UUID.randomUUID().toString())
                .setValue(Message(id, senderUsername, text, timestamp,false)).await()
        return if (id != null) timestamp else 0L
    }
    suspend fun editMessage(chatId: String, message: Map<String, Message>) {
        if (userId == null) return
        messagesRef.child(chatId)
            .child(message.keys.first())
            .setValue(message.values.first().copy(edited = true)).await()
    }
    suspend fun deleteMessage(chatId: String, message: Map<String, Message>) {
        if (userId == null) return
        messagesRef.child(chatId)
            .child(message.keys.first())
            .removeValue().await()
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
    fun formatDate(timestamp: Long) = formatDateUseCase(timestamp)
    //scroll to bottom if the message before the last one is 100% visible at the time of receiving a new one
    suspend fun scrollToBottom(messages: Map<String, Message>, listState: LazyListState, firstVisibleItem: LazyListItemInfo) {
        val viewportHeight = listState.layoutInfo.viewportEndOffset
        val itemTop = firstVisibleItem.offset
        val itemBottom = itemTop + firstVisibleItem.size
        val isCompletelyVisible = itemTop >= 0 && itemBottom <= viewportHeight
        if (messages.isNotEmpty() &&
            (firstVisibleItem.index == messages.entries.indexOf(messages.entries.first())+1 &&
                            isCompletelyVisible)) {
            listState.animateScrollToItem(0)
        }
    }

    fun handleDynamicMessageLoading(
        loadOnActivityResume: Boolean = false,
        topMessageIndex: Int,
        lastVisibleItemIndex: Int?,
        onRemove: () -> Unit,
        onLoad: (Int) -> Unit) {
        if ((lastVisibleItemIndex != null && lastVisibleItemIndex >= topMessageIndex-1 || topMessageIndex == 0)
            && !loadOnActivityResume) {
            if (lastVisibleItemIndex != null) onRemove()
            onLoad(10)
        }
        if (loadOnActivityResume) {
            //load same messages as before by not increasing topMessageBatchIndex
            onLoad(0)
        }
    }
}
