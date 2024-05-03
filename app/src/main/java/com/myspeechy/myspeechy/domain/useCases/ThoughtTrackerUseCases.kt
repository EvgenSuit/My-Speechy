package com.myspeechy.myspeechy.domain.useCases

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class IsDateEqualToCurrentUseCase(private val dateTime: LocalDateTime = LocalDateTime.now()) {
    operator fun invoke(date: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = sdf.parse(date) ?: return false
        val date = Instant.ofEpochMilli(parsedDate.time)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        return (date.year == dateTime.year &&
                date.month == dateTime.month &&
                date.dayOfMonth == dateTime.dayOfMonth)
    }
}

class GetCurrentDateUseCase(private val dateTime: LocalDateTime = LocalDateTime.now()) {
    operator fun invoke(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val localDateTime = dateTime.format(formatter)
        return localDateTime
    }
}