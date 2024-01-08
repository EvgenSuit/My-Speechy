package com.example.myspeechy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "database")
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val unit: Int,
    val category: Int,
    val title: String,
    val text: String,
    val isComplete: Int,
    val containsImages: Int
)