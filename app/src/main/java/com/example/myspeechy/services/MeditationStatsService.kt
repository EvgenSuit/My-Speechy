package com.example.myspeechy.services

import android.util.Log
import com.example.myspeechy.data.LessonItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

interface MeditationStatsService {
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    fun updateStats(date: String, minutes: Int) {
        Firebase.firestore.collection(userId).document("meditation")
            .collection(date).document("items").set(mapOf(
                "minutes" to minutes))
    }
    fun trackRemoteStats(date: String, onDataReceived: (Int) -> Unit) {
        val docRef = Firebase.firestore.collection(userId).document("meditation")
            .collection(date).document("items")
        docRef.addSnapshotListener{doc, e ->
            if (e != null || doc == null) {
                //Todo implement make text with error message
                Log.w("LISTEN ERROR", e)
                onDataReceived(0)
                return@addSnapshotListener
            }
            val minutes = doc.data?.get("minutes")!! as Long
            onDataReceived(minutes.toInt())
        }
    }
}

class MeditationStatsServiceImpl: MeditationStatsService