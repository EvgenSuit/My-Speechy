package com.example.myspeechy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LessonFlags(
    @PrimaryKey
    val id: Int,
    val isComplete: Int = 1
)
