package com.example.myspeechy.helpers

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

enum class LessonCategories {
    PHYSICAL,
    BREATHING,
    PSYCHOLOGICAL,
    MEDITATION,
    MINDFULNESS,
    READING,
    NONE,
}

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
        fun categoryMapperReverse(category: String): Int {
            return when(category) {
                "Physical" -> 0
                "Breathing" -> 1
                "Psychological" -> 2
                "Meditation" -> 3
                "Mindfulness" -> 4
                else -> 5
            }
        }

    fun loadImgFromAsset(imgPath: String, assetManager: AssetManager): ImageBitmap {
        val inputStream = assetManager.open(imgPath)
        return BitmapFactory.decodeStream(inputStream).asImageBitmap()
    }

}