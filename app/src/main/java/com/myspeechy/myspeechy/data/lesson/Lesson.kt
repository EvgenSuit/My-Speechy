package com.myspeechy.myspeechy.data.lesson

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "database")
data class Lesson(
    @PrimaryKey
    val id: Int,
    val unit: Int,
    val category: Int,
    val title: String,
    val text: String,
    val isComplete: Int,
    val isAvailable: Int,
    val containsImages: Int
)