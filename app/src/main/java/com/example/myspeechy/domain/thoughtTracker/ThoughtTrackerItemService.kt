package com.example.myspeechy.domain.thoughtTracker

import com.example.myspeechy.data.thoughtTrack.ThoughtTrack
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class ThoughtTrackerItemService(
    private val usersRef: CollectionReference,
    private val auth: FirebaseAuth
) {
    fun listenForData(
        timestamp: Long,
        onError: (FirebaseFirestoreException.Code) -> Unit,
        onData: (ThoughtTrack?) -> Unit) {
        val userId = auth.uid ?: return
        usersRef.document(userId)
            .collection("thoughtTracks").document(timestamp.toString()).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.code)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    onData(snapshot.toObject(ThoughtTrack::class.java))
                }
            }
    }
    suspend fun saveData(
        timestamp: Long,
        track: ThoughtTrack) {
        val userId = auth.uid ?: return
        usersRef.document(userId).collection("thoughtTracks")
            .document(timestamp.toString()).set(track.copy(questions = track.questions as LinkedHashMap<String, Int>)).await()
    }
}