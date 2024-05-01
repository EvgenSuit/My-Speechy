package com.myspeechy.myspeechy.presentation.chat

import java.util.Locale

fun String.formatStorageErrorMessage() = this.split(" ").joinToString("_")
    .uppercase(Locale.ROOT).dropLast(1)
fun String.getOtherUserId(userId: String) = this.split("_").first { it != userId }