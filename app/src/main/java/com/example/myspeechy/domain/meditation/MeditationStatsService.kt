package com.example.myspeechy.domain.meditation

import com.example.myspeechy.domain.MeditationTimeExceededException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object MeditationConfig {
    const val MIN_MEDITATION_POINT = 0
    const val MAX_MEDITATION_POINT = 10
    const val STEP_SIZE = 1
}

class MeditationStatsServiceImpl(
    private val firestoreRef: FirebaseFirestore,
    private val auth: FirebaseAuth) {

    private lateinit var statsListener: ListenerRegistration
    suspend fun updateStats(date: String, seconds: Int) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val ref = firestoreRef.collection("users").document(userId)
                .collection("meditation").document(date)
            val totalMeditationTime = (ref.get().await().data?.values?.first() as Long?)?.toInt() ?: 0
            if (totalMeditationTime > 60*MeditationConfig.MAX_MEDITATION_POINT)
                throw MeditationTimeExceededException("Unable to save progress. You've reached your maximum daily meditation time")
            ref.set(mapOf("seconds" to (totalMeditationTime+seconds))).await()
        }
    }
    fun trackRemoteStats(
        coroutineScope: CoroutineScope,
        remove: Boolean,
        onListenError: (e: FirebaseFirestoreException.Code) -> Unit,
        onOtherError: (e: String) -> Unit,
        onDataReceived: (Map<String, Int>) -> Unit) {
        if (remove) {
            statsListener.remove()
            return
        }
        val userId = auth.currentUser?.uid ?: return
        val ref = firestoreRef.collection("users").document(userId).collection("meditation")
        statsListener = ref.addSnapshotListener{ docs, e ->
            if (docs == null || docs.isEmpty || docs.documents.isEmpty()) {
                onDataReceived(mapOf())
                return@addSnapshotListener
            }
            if (e != null) {
                onListenError(e.code)
                onDataReceived(mapOf())
                return@addSnapshotListener
            }
            val documents = docs.documents
            val lastDocs = documents.takeLast(4)
            try {
                coroutineScope.launch {
                    if (documents.size > 4) {
                        deleteOldDocs(firestoreRef, documents)
                    }
                    val statsMap = buildMap {
                        lastDocs.forEach {doc ->
                            // delete only if there's at least 1 document other than "dummy"
                            if (lastDocs.size > 1 && doc.id == "dummy") {
                                ref.document(doc.id).delete().await()
                            }
                            if (doc.id != "dummy") {
                                //put date and seconds
                                put(doc.id, (doc.data!!.values.first() as Long).toInt())
                            }
                        }
                    }
                    onDataReceived(statsMap)
                }
            } catch (e: Exception) {
                onOtherError(e.message!!)
            }
        }
    }
    private suspend fun deleteOldDocs(
        ref: FirebaseFirestore,
        documents: List<DocumentSnapshot>) {
        val oldDocs = documents.take(documents.size-4)
        val batch = ref.batch()
        for (doc in oldDocs) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}
