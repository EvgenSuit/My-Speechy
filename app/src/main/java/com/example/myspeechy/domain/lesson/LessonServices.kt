package com.example.myspeechy.domain.lesson

import android.content.res.AssetManager
import androidx.compose.ui.graphics.ImageBitmap
import com.example.myspeechy.data.lesson.Lesson
import com.example.myspeechy.data.lesson.LessonItem
import com.example.myspeechy.helpers.LessonServiceHelpers
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

interface LessonService {
    val lessonServiceHelpers: LessonServiceHelpers
        get() = LessonServiceHelpers()
    val userId: String?
        get() = Firebase.auth.currentUser?.uid

    fun convertToLessonItem(lesson: Lesson): LessonItem {
        return LessonItem(
            lesson.id,
            lesson.unit,
            lessonServiceHelpers.categoryMapper(lesson.category),
            lesson.title,
            lesson.text,
            lesson.isComplete == 1,
            lesson.isAvailable == 1,
            lesson.containsImages == 1
        )
    }
    fun parseImgFromText(lessonItem: LessonItem, imgs: List<String>): List<String> {
        return listOf()
    }
    /**
    This method maps the names of images to their bitmap representations,
     which then helps to display them in an annotatedString
     */
    fun loadImgFromAsset(lessonItem: LessonItem, imgs: List<String>, dir: String, assetManager: AssetManager): Map<String, ImageBitmap> {
        return mapOf()
    }
    fun markAsComplete(lessonItem: LessonItem) {
        if (userId != null) {
            Firebase.firestore.collection("users").document(userId!!).collection("lessons")
                .document(lessonItem.id.toString()).set(mapOf("id" to lessonItem.id))
        }
    }
    fun trackRemoteProgress(onListenError: (errorCode: FirebaseFirestoreException.Code) -> Unit,
                            onOtherError: (String) -> Unit,
                            onDataReceived: (List<Int>) -> Unit) {}
}

class RegularLessonServiceImpl: LessonService {
    override fun parseImgFromText(lessonItem: LessonItem, imgs: List<String>): List<String> {
        val textSplit = lessonItem.text.split("\n")
        val newText = mutableListOf<String>()
        var i = 0
        textSplit.forEach {
            if (it.contains("<Image>")) {
                newText.add(imgs[i])
                i++
            } else {
                newText.add(it)
            }
        }
        return newText
    }

    override fun loadImgFromAsset(lessonItem: LessonItem, imgs: List<String>,
                                  dir: String, assetManager: AssetManager): Map<String, ImageBitmap> {
        val imgsMap = mutableMapOf<String, ImageBitmap>()
        if (lessonItem.containsImages) {
            for (i in imgs.indices) {
                imgsMap.put(imgs[i],
                    lessonServiceHelpers.loadImgFromAsset(dir + imgs[i], assetManager))
            }
        }
        return imgsMap
    }
}

class MainLessonServiceImpl(private val firestoreRef: FirebaseFirestore): LessonService {
    override fun trackRemoteProgress(onListenError: (errorCode: FirebaseFirestoreException.Code) -> Unit,
                                     onOtherError: (String) -> Unit,
        onDataReceived: (List<Int>) -> Unit) {
        if (userId == null) return
        val docRef = firestoreRef.collection("users").document(userId!!).collection("lessons")
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
                val data = mutableListOf<Int>()
                try {
                    for (doc in snapshot.documents) {
                        val id = doc.id.toIntOrNull()
                        if (id != null) {
                            data.add(id)
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

class ReadingLessonServiceImpl: LessonService

class MeditationLessonServiceImpl: LessonService