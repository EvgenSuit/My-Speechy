package com.example.myspeechy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MeditationStats(
    @PrimaryKey(autoGenerate = false)
    val date: String,
    val minutes: Int
)
