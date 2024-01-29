package com.example.myspeechy.services

import android.content.res.AssetManager
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonItem
import com.example.myspeechy.helpers.LessonServiceHelpers
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private val userId = Firebase.auth.currentUser!!.uid
interface LessonService {
    val lessonServiceHelpers: LessonServiceHelpers
        get() = LessonServiceHelpers()

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
    fun convertToLesson(lessonItem: LessonItem): Lesson {
        return Lesson(
            lessonItem.id,
            lessonItem.unit,
            lessonServiceHelpers.categoryMapperReverse(lessonItem.category),
            lessonItem.title,
            lessonItem.text,
            if (lessonItem.isComplete) 1 else 0,
            if (lessonItem.isAvailable) 1 else 0,
            if (lessonItem.containsImages) 1 else 0
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
        val lesson = convertToLesson(lessonItem.copy(isComplete = true))
        Firebase.firestore.collection(userId)
            .document(lesson.id.toString()).set(mapOf("isComplete" to true))
    }
    fun trackRemoteProgress(lessonId: Int, onDataReceived: (Map<String, Boolean>) -> Unit) {

    }
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

class MainLessonServiceImpl: LessonService {
    override fun trackRemoteProgress(lessonId: Int, onDataReceived: (Map<String, Boolean>) -> Unit) {
        val docRef = Firebase.firestore.collection(userId).document(lessonId.toString())
        docRef.addSnapshotListener{snapshot, e ->
            if (e != null) {
                Log.w("LISTEN ERROR", e)
            }
            if (snapshot != null && snapshot.exists()) {
                onDataReceived(snapshot.data as Map<String, Boolean>)
            } else {
                onDataReceived(mapOf("isComplete" to false))
            }
        }
    }
}

class ReadingLessonServiceImpl: LessonService