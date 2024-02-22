package com.example.myspeechy.services.chat

import android.util.Log
import com.example.myspeechy.data.chat.Chat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private val database = Firebase.database.reference
interface ChatsService {
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    val userRef: DatabaseReference
        get() = database.child("users").child(userId)
    fun listener(
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
    fun membershipStateChildListener(
        onAdded: (String) -> Unit,
        onChanged: (String) -> Unit,
        onRemoved: (String) -> Unit,
        onCancelled: (Int) -> Unit): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onAdded(snapshot.key!!)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                onChanged(snapshot.key!!)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                onRemoved("")
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error.code)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }
    }

    fun searchChatByTitle(title: String,
                          onCancelled: (Int) -> Unit,
                          onDataReceived: (DataSnapshot) -> Unit) {
        database.child("public_chats")
            .orderByChild("title")
            .equalTo(title)
            .addValueEventListener(listener(onCancelled, onDataReceived))
    }
    fun publicChatsStateListener(onAdded: (String) -> Unit,
                                 onChanged: (String) -> Unit,
                                 onRemoved: (String) -> Unit,
                                 onCancelled: (Int) -> Unit,
                                 remove: Boolean)
    fun privateChatsStateListener(onAdded: (String) -> Unit,
                                  onChanged: (String) -> Unit,
                                  onRemoved: (String) -> Unit,
                                  onCancelled: (Int) -> Unit,
                                  remove: Boolean)
    fun publicChatListener(id: String,
                           onDataReceived: (DataSnapshot) -> Unit,
                           onCancelled: (Int) -> Unit,
                           remove: Boolean)
    fun privateChatListener(id: String,
                            onDataReceived: (DataSnapshot) -> Unit,
                            onCancelled: (Int) -> Unit,
                            remove: Boolean)
}
class ChatsServiceImpl: ChatsService {
    private var publicChatsStateListener: ChildEventListener? = null
    private var privateChatsStateListener: ChildEventListener? = null
    private var publicChatsListeners: MutableMap<String, ValueEventListener> = mutableMapOf()
    private var privateChatsListeners: MutableMap<String, ValueEventListener> = mutableMapOf()

    override fun publicChatsStateListener(
        onAdded: (String) -> Unit,
        onChanged: (String) -> Unit,
        onRemoved: (String) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean) {
        val ref = userRef.child("public_chats")
        if (remove && publicChatsStateListener != null) {
            ref.removeEventListener(publicChatsStateListener!!)
        } else {
            publicChatsStateListener = membershipStateChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(publicChatsStateListener!!)
        }
    }


    override fun privateChatsStateListener(
        onAdded: (String) -> Unit,
        onChanged: (String) -> Unit,
        onRemoved: (String) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean) {
        val ref = userRef.child("private_chats")
        if (remove && privateChatsStateListener != null) {
            ref.removeEventListener(privateChatsStateListener!!)
        } else {
            privateChatsStateListener = membershipStateChildListener(onAdded, onChanged, onRemoved, onCancelled)
            ref.addChildEventListener(privateChatsStateListener!!)
        }
    }

    override fun publicChatListener(
        id: String,
        onDataReceived: (DataSnapshot) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean
    ) {
        val ref = database.child("public_chats").child(id)
        val currListener = publicChatsListeners[id]
        if (remove && currListener != null) {
            ref.removeEventListener(currListener)
            publicChatsListeners.remove(id)
        } else {
            publicChatsListeners[id] = listener(onCancelled, onDataReceived)
            ref.addValueEventListener(publicChatsListeners[id]!!)
        }
    }

    override fun privateChatListener(
        id: String,
        onDataReceived: (DataSnapshot) -> Unit,
        onCancelled: (Int) -> Unit,
        remove: Boolean
    ) {
        val ref = database.child("private_chats")
            .child(userId)
            .child(id)
        val currListener = privateChatsListeners[id]
        if (remove && currListener != null) {
            ref.removeEventListener(currListener)
            privateChatsListeners.remove(id)
        } else {
            privateChatsListeners[id] = listener(onCancelled, onDataReceived)
            ref.addValueEventListener(privateChatsListeners[id]!!)
        }
    }
}