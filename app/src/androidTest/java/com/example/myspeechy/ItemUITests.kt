package com.example.myspeechy

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemUITests {
    @Test
    fun parseImgInText() {
        val containsImgs = 1
        if (containsImgs != 1) return
        val unit = 1
        val category = "psychological"
        val text = "<Image>\n" +
                "Try to lift up your shoulders and tighten your stomach and lips. Is this feeling familiar to you?\n" +
                "<Image>\n" +
                "If yes, you could have stuttering."
        val textSplit = text.split("\n")
        var newText = ""
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imgFiles = context.assets.list("imgs/unit$unit/$category")!!.toList()
        var i = 0
        textSplit.forEach {
            if (it.contains("<Image>")) {
                newText += imgFiles[i]
                i++
            } else {
                newText += it
            }
        }
        println(newText)
        println(textSplit.size)
    }
}