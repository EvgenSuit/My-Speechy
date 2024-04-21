package com.example.myspeechy.data.thoughtTrack

import androidx.annotation.Keep

@Keep
data class ThoughtTrack(
    val questions: Map<String, Int> = mapOf(
        "How well were you feeling today?" to -1,
        "How good was your speech today?" to -1,
        "How confident were you today?" to -1,
        "How much tension in your body did you feel today?" to -1
    ),
    val thoughts: String? = null)

@Keep
data class ThoughtTrackItem(val timestamp: Long = 0)