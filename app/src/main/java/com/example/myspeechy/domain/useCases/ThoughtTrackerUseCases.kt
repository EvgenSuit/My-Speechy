package com.example.myspeechy.domain.useCases

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class IsDateEqualToCurrentUseCase {
    operator fun invoke(timestamp: Long): Boolean {
        val currentDate = LocalDateTime.now()
        val date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        return (date.year == currentDate.year &&
                date.month == currentDate.month &&
                date.dayOfMonth == currentDate.dayOfMonth)
    }
}

class GetCurrentDateInTimestampUseCase {
    operator fun invoke(): Long {
        val currDate = LocalDateTime.now()
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(currDate)
        return currDate.toInstant(zoneOffset).toEpochMilli()
    }
}