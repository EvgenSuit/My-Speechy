package com.example.myspeechy.domain.thoughtTracker

import android.media.metrics.Event
import android.util.Log
import com.example.myspeechy.data.thoughtTrack.ThoughtTrack
import com.example.myspeechy.data.thoughtTrack.ThoughtTrackItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject

class ThoughtTrackerService(
    private val usersRef: CollectionReference,
    private val auth: FirebaseAuth) {
    private var eventListener: ListenerRegistration? = null



    // TODO remove doc change listen
    fun listenForData(onError: (FirebaseFirestoreException.Code) -> Unit,
                      onData: (List<ThoughtTrackItem>) -> Unit,
                      onRemove: (Long) -> Unit,
                      remove: Boolean) {
        val userId = auth.uid ?: return
        if (remove) {
            eventListener?.remove()
            return
        }
        eventListener = usersRef.document(userId)
            .collection("thoughtTracks").addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e.code)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.REMOVED) {
                        onRemove(change.document.id.toLong())
                    }
                }
                onData(snapshot.documents.map { doc -> ThoughtTrackItem(doc.id.toLong()) })
            }
        }
    }
}