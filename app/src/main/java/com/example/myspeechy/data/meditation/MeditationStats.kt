package com.example.myspeechy.data.meditation

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MeditationStats(
    @PrimaryKey(autoGenerate = false)
    val date: String,
    val seconds: Int
)
