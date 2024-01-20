package com.example.myspeechy.services

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.myspeechy.data.Lesson
import com.example.myspeechy.data.LessonRepository
import com.example.myspeechy.helpers.LessonServiceHelpers
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

data class LessonItem(
    var id: Int = 0,
    val unit: Int = 1,
    val category: String = "",
    val title: String = "",
    val text: String = "",
    val isComplete: Boolean = false,
    val isAvailable: Boolean = false,
    val containsImages: Boolean = false
)

interface LessonService {
    fun convertToLessonItem(lesson: Lesson): LessonItem
    fun parseImgFromText(lessonItem: LessonItem, imgs: List<String>): List<String>
    fun convertToLesson(lessonItem: LessonItem): Lesson
    fun saveProgressRemotely(lessonId: Int)
    fun trackRemoteProgress(lessonId: Int, onDataReceived: (Map<String, Boolean>) -> Unit)
}

class LessonServiceImpl(private val userId: String): LessonService {
    val lessonServiceHelpers = LessonServiceHelpers()
    private val firestore = Firebase.firestore

    override fun saveProgressRemotely(lessonId: Int) {
        firestore.collection(userId)
            .document(lessonId.toString()).set(mapOf("isComplete" to true))
    }

    override fun trackRemoteProgress(lessonId: Int, onDataReceived: (Map<String, Boolean>) -> Unit) {
        val docRef = firestore.collection(userId).document(lessonId.toString())
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

    override fun convertToLessonItem(lesson: Lesson): LessonItem {
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

    override fun convertToLesson(lessonItem: LessonItem): Lesson {
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



}