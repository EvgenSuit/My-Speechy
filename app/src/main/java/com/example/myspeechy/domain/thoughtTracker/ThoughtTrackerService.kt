package com.example.myspeechy.domain.thoughtTracker

import com.example.myspeechy.data.thoughtTrack.ThoughtTrack
import com.example.myspeechy.data.thoughtTrack.ThoughtTrackItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

class ThoughtTrackerService(
    private val usersRef: CollectionReference,
    private val auth: FirebaseAuth) {
    private var eventListener: ListenerRegistration? = null

    fun listenForData(onError: (FirebaseFirestoreException.Code) -> Unit,
                      onData: (List<ThoughtTrackItem>) -> Unit,
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
                onData(snapshot.documents.map { doc -> ThoughtTrackItem(doc.id) })
            }
        }
    }
}