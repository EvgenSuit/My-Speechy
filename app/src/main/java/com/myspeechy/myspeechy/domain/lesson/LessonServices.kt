package com.myspeechy.myspeechy.domain.lesson

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.myspeechy.myspeechy.data.DataStoreManager
import com.myspeechy.myspeechy.data.lesson.Lesson
import com.myspeechy.myspeechy.data.lesson.LessonCategories
import com.myspeechy.myspeechy.data.lesson.LessonItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface LessonService {
    val userId: String?
    val usersRef: CollectionReference
    val dataStoreManager: DataStoreManager

    fun categoryToDialogText(category: LessonCategories): String {
        return when(category) {
            LessonCategories.PSYCHOLOGICAL -> "This type of exercise is designed to help you fight your mental battles."
            LessonCategories.READING -> "With this type of exercise you can practice your slow speaking skill " +
                    "which is useful when it comes to managing stuttering. Read the text out loud. Use the slider at the bottom " +
                    "to control the speed."
            LessonCategories.MEDITATION -> "This type of exercise enhances your focus and cultivates inner peace."
            else -> ""
        }
    }

    fun convertToLessonItem(lesson: Lesson): LessonItem {
        return LessonItem(
            lesson.id,
            lesson.unit,
            LessonCategories.entries[lesson.category],
            lesson.title,
            lesson.text,
            lesson.isComplete == 1,
            lesson.isAvailable == 1,
            lesson.containsImages == 1
        )
    }
    suspend fun editWelcomeDialogBoxShown(category: LessonCategories) {
        dataStoreManager.editWelcomeDialogBoxShown(category)
    }
    suspend fun collectDialogBoxShown(
        category: LessonCategories,
        onData: (Boolean) -> Unit) {
        dataStoreManager.collectWelcomeDialogBoxShow {
            onData(it.contains(category.name))
        }
    }
    suspend fun markAsComplete(lessonItem: LessonItem) {
        if (userId != null) {
            usersRef.document(userId!!).collection("lessons")
                .document(lessonItem.id.toString()).set(mapOf("id" to lessonItem.id)).await()
        }
    }
    fun trackRemoteProgress(
        coroutineScope: CoroutineScope,
        onListenError: (errorCode: FirebaseFirestoreException.Code) -> Unit,
                            onOtherError: (String) -> Unit,
                            onDataReceived: (List<Int>) -> Unit) {}
}

class RegularLessonServiceImpl(
    override val usersRef: CollectionReference,
    override val dataStoreManager: DataStoreManager,
    auth: FirebaseAuth
): LessonService {
    override val userId = auth.currentUser?.uid
}

class MainLessonServiceImpl(override val usersRef: CollectionReference,
                            override val dataStoreManager: DataStoreManager,
                            auth: FirebaseAuth): LessonService {
    override val userId = auth.currentUser?.uid
    override fun trackRemoteProgress(
        coroutineScope: CoroutineScope,
        onListenError: (errorCode: FirebaseFirestoreException.Code) -> Unit,
                                     onOtherError: (String) -> Unit,
        onDataReceived: (List<Int>) -> Unit) {
        if (userId == null) return
        val docRef = usersRef.document(userId).collection("lessons")
        docRef.addSnapshotListener{snapshot, e ->
            if (e != null) {
                onListenError(e.code)
                return@addSnapshotListener
            }
            else if (snapshot == null || snapshot.isEmpty) {
                onListenError(FirebaseFirestoreException.Code.NOT_FOUND)
                return@addSnapshotListener
            }
           else {
                coroutineScope.launch {
                    val data = mutableListOf<Int>()
                    try {
                        for (doc in snapshot.documents) {
                            val id = doc.id
                            if (id == "dummy" && snapshot.documents.size > 1) {
                                docRef.document(id).delete().await()
                            }
                            if (id != "dummy") {
                                data.add(id.toInt())
                            }
                        }
                    } catch (e: Exception) {
                        onOtherError(e.message!!)
                    }
                    onDataReceived(data)
                }
            }
        }
    }
}

class ReadingLessonServiceImpl(
    override val usersRef: CollectionReference,
    override val dataStoreManager: DataStoreManager,
    auth: FirebaseAuth
): LessonService {
    override val userId = auth.currentUser?.uid
}

class MeditationLessonServiceImpl(
    override val usersRef: CollectionReference,
    override val dataStoreManager: DataStoreManager,
    auth: FirebaseAuth
): LessonService {
    override val userId = auth.currentUser?.uid
}
