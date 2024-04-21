package com.example.myspeechy.domain

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class DateFormatter {
    companion object {
        fun convertFromTimestamp(timestamp: Long): String {
            var targetDateFormat = ""
            val currentDate = LocalDateTime.now()
            val messageDateFormatted = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (currentDate.year != messageDateFormatted.year) targetDateFormat += "yyyy "
            if (currentDate.dayOfMonth != messageDateFormatted.dayOfMonth) targetDateFormat += "MMM dd "
            targetDateFormat += "HH:mm"
            return SimpleDateFormat(targetDateFormat, Locale.getDefault()).format(timestamp)
        }
        fun convertFromTimestampThoughtTracker(timestamp: Long): String {
            var targetDateFormat = ""
            val currentDate = LocalDateTime.now()
            val messageDateFormatted = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (currentDate.year != messageDateFormatted.year) targetDateFormat += "yyyy "
            return if (currentDate.year == messageDateFormatted.year &&
                currentDate.month == messageDateFormatted.month &&
                currentDate.dayOfMonth == messageDateFormatted.dayOfMonth) "Today"
            else SimpleDateFormat("${targetDateFormat}MMM dd", Locale.getDefault()).format(timestamp)
        }
    }
}