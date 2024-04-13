package com.example.myspeechy.presentation

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes

sealed class UiText {
    class StringResource(@StringRes val id: Int): UiText() {
        data class DynamicString(val s: String): UiText()
        fun asString(context: Context): String = context.getString(id)
    }
}