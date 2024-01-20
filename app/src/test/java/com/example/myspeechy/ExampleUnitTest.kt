package com.example.myspeechy

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ApplicationProvider
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

}