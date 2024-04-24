package com.example.myspeechy.domain.thoughtTracker

import com.example.myspeechy.data.thoughtTrack.ThoughtTrack
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

class ThoughtTrackerItemService(
    private val usersRef: CollectionReference,
    private val auth: FirebaseAuth
) {
    fun listenForData(
        date: String,
        onError: (FirebaseFirestoreException.Code) -> Unit,
        onData: (ThoughtTrack?) -> Unit) {
        val userId = auth.uid ?: return
        usersRef.document(userId)
            .collection("thoughtTracks").document(date).addSnapshotListener { snapshot, e ->
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
        date: String,
        track: ThoughtTrack) {
        val userId = auth.uid ?: return
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val localDateTime = LocalDate.parse(date).atStartOfDay()
        val utcDateTime = localDateTime.atOffset(ZoneOffset.UTC)
        val formattedDate = utcDateTime.format(formatter)
        usersRef.document(userId).collection("thoughtTracks")
            .document(formattedDate).set(track).await()
    }
}