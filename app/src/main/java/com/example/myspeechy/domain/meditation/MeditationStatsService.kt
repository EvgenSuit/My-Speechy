package com.example.myspeechy.domain.meditation

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

interface MeditationStatsService {
    val userId: String
        get() = Firebase.auth.currentUser!!.uid
    fun updateStats(date: String, minutes: Int) {
        Firebase.firestore.collection(userId).document("meditation")
            .collection("items").document(date).set(mapOf(
                "minutes" to minutes))
    }
    fun trackRemoteStats(onListenError: () -> Unit, onDataReceived: (Map<String, Int>) -> Unit) {
        val docRef = Firebase.firestore.collection(userId).document("meditation")
            .collection("items")
        docRef.addSnapshotListener{docs, e ->
            if (docs == null || docs.isEmpty || docs.documents.isEmpty()) {
                onDataReceived(mapOf())
                return@addSnapshotListener
            }
            if (e != null) {
                onListenError()
                onDataReceived(mapOf())
                return@addSnapshotListener
            }
            val statsMap = buildMap {
                    docs.documents.takeLast(4).forEach {doc ->
                        put(doc.id, (doc.data!!.values.first() as Long).toInt())
                    }
                }
            onDataReceived(statsMap)
        }
    }
}

class MeditationStatsServiceImpl: MeditationStatsService