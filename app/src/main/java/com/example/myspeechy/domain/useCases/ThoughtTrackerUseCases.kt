package com.example.myspeechy.domain.useCases

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class IsDateEqualToCurrentUseCase {
    operator fun invoke(date: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = LocalDateTime.now()
        val parsedDate = sdf.parse(date) ?: return false
        val date = Instant.ofEpochMilli(parsedDate.time)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        return (date.year == currentDate.year &&
                date.month == currentDate.month &&
                date.dayOfMonth == currentDate.dayOfMonth)
    }
}

class GetCurrentDateUseCase {
    operator fun invoke(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val localDateTime = LocalDateTime.now().format(formatter)
        return localDateTime
    }
}