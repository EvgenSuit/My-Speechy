package com.example.myspeechy.helpers

class LessonServiceHelpers {
        fun categoryMapper(category: Int): String {
            return when(category) {
                0 -> "Physical"
                1 -> "Breathing"
                2 -> "Psychological"
                3 -> "Meditation"
                4 -> "Mindfulness"
                else -> "Reading"
            }
        }


}