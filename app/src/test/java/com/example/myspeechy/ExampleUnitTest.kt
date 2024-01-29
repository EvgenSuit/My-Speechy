package com.example.myspeechy

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun parseImgInText() {
        val text = "This is an image: <Image> Cool, isn't it?"
        val textSplit = text.split("<Image>")
        assertEquals(textSplit[0] + textSplit[1], "This is an image:  Cool, isn't it?")
    }

    @Test
    fun movePointerInReadingLessonItem() = runTest {
        val speed = 1.2f
        var index = 0
        val text = "Some text"
        while(index < text.length) {
            delay((300/speed).toLong())
            index++
            assertEquals(text.substring(0, index).length, index)
        }
    }

}